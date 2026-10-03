# Deploying Cookify on AWS

```
Browser ──HTTPS──► CloudFront  https://<id>.cloudfront.net
                     ├─ /media/*        → S3 bucket (private, Origin Access Control)
                     ├─ /api/*, /ws/*   → EC2 (Nginx → Spring Boot :8080)   no caching
                     └─ /* (default)    → EC2 (Nginx → frontend files)
EC2 t3.micro ──► RDS PostgreSQL db.t3.micro (private)
             ──► S3 (media), SES (email)   via the instance role — no access keys
```

Region used throughout: **ap-south-1 (Mumbai)**. Everything fits the 12-month free tier.

## 0. Safety first
- Billing → **Budgets** → create a monthly cost budget of **$5** with an email alert.

## 1. SES — email
1. SES → **Identities** → *Create identity* → **Email address** → your sender address → verify the link you receive.
2. New accounts are in the **sandbox**: they can only send *to* verified addresses. Verify the inboxes you will test with
   (or request production access: SES → Account dashboard → *Request production access*).

## 2. S3 — media bucket
1. S3 → *Create bucket* → name `cookify-media-<yourname>`, region ap-south-1.
2. Keep **Block all public access = ON** (CloudFront reads it privately). Create.

## 3. RDS — PostgreSQL
1. RDS → *Create database* → **Standard create** → PostgreSQL → template **Free tier**.
2. DB instance identifier `cookify-db`, master username `cookify`, set a strong password (save it).
3. Instance `db.t3.micro`, storage 20 GB gp3, **Public access: No**.
4. Additional configuration → **Initial database name: `cookify`**.
5. Create, then note the **Endpoint**.

## 4. IAM — role for the server
1. IAM → Policies → *Create policy* → JSON → paste [`iam-policy.json`](iam-policy.json) (replace `cookify-media-YOURNAME`) → name `cookify-app`.
2. IAM → Roles → *Create role* → trusted entity **EC2** → attach `cookify-app` → name `cookify-ec2`.

## 5. EC2 — application server
1. EC2 → *Launch instance* → name `cookify` → **Amazon Linux 2023** → **t3.micro**.
2. Key pair → create `cookify` (.pem) and keep it safe (`chmod 400 cookify.pem`).
3. Network → create security group `cookify-web`:
   - SSH (22) from **My IP**
   - HTTP (80) from **Anywhere** (CloudFront connects over HTTP; viewers only ever use HTTPS)
4. Advanced → **IAM instance profile: `cookify-ec2`**. Launch, then note the **Public IPv4 DNS**.
5. RDS → your DB → *Connectivity & security* → its security group → *Edit inbound rules* →
   add PostgreSQL (5432) with source = security group **`cookify-web`**.

## 6. First deploy (from the project root on your machine)
```bash
export EC2_HOST=ec2-xx-xx-xx-xx.ap-south-1.compute.amazonaws.com
export SSH_KEY=~/keys/cookify.pem
SETUP=1 ./infra/scripts/deploy.sh      # installs Java + Nginx, then stops at the env file
ssh -i $SSH_KEY ec2-user@$EC2_HOST     # then: sudo nano /etc/cookify.env  (see cookify.env.example)
```
Fill `/etc/cookify.env` (DB endpoint + password, `JWT_SECRET` from `openssl rand -base64 48`,
bucket, SES sender, `DEMO_DATA=true` for the first start). `APP_URL` comes from step 7 — put a placeholder
for now. Then run `./infra/scripts/deploy.sh` again; it prints the health check when the API is up.

## 7. CloudFront — HTTPS and one public URL
1. CloudFront → *Create distribution*.
2. **Origin 1 (default)**: origin domain = the EC2 *Public IPv4 DNS*, protocol **HTTP only**, port 80.
   Default behavior: viewer protocol **Redirect HTTP to HTTPS**, allowed methods **GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE**,
   cache policy **CachingDisabled**, origin request policy **AllViewer**.
3. *Create* (wait until deployed), then add behaviors:
   - `/api/*` → EC2 origin, all methods, **CachingDisabled**, **AllViewer**
   - `/ws*` → EC2 origin, all methods, **CachingDisabled**, **AllViewer** (WebSockets work automatically)
   - `/media/*` → add **Origin 2** = the S3 bucket with **Origin access control** (create new OAC);
     GET/HEAD, cache policy **CachingOptimized**. Copy the bucket policy CloudFront offers
     (same as [`s3-bucket-policy.json`](s3-bucket-policy.json)) into S3 → bucket → Permissions → Bucket policy.
4. Copy the distribution domain `https://<id>.cloudfront.net` into `APP_URL` in `/etc/cookify.env`,
   then `sudo systemctl restart cookify`.

### 7b. Until CloudFront is available — HTTPS on the server
New AWS accounts can't create CloudFront distributions until AWS Support verifies them. Meanwhile:
1. Attach an **Elastic IP** to the instance and open **80 and 443** to everyone in `cookify-web`.
2. Use the free wildcard DNS name `<a-b-c-d>.sslip.io` (the Elastic IP with dashes) and get a Let's Encrypt
   certificate: certbot in a venv (`/opt/certbot`), `certbot --nginx -d <a-b-c-d>.sslip.io --redirect`,
   plus a systemd timer running `certbot renew` twice a day.
3. In `/etc/cookify.env`: `APP_URL=https://<a-b-c-d>.sslip.io` and **`MEDIA_VIA_APP=true`** — the bucket stays
   private and the app streams `/media/**` from S3 with its instance role (range requests work for videos).

Switching to CloudFront later: create the distribution (step 7, origin = the EC2 public DNS), set
`APP_URL` to the CloudFront URL, `MEDIA_VIA_APP=false`, restart.

## 8. Check
- `https://<id>.cloudfront.net` loads; sign up → 2FA email arrives (verified inbox in sandbox).
- Upload a recipe photo/video → it appears (served from `/media/...`).
- Chat between two browsers is live (WebSocket through CloudFront).
- After the first start with demo data, set `DEMO_DATA=false` and restart.

## Updating
```bash
./infra/scripts/deploy.sh             # build → upload → restart → health check
```

## Useful commands on the server
```bash
sudo journalctl -u cookify -f         # application logs
sudo systemctl restart cookify
sudo nginx -t && sudo systemctl reload nginx
```

## Saving money
Stop the EC2 instance and the RDS database when not demoing (RDS restarts itself after 7 days if stopped).
