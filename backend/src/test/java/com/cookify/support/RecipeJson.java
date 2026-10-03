package com.cookify.support;

/** Sample recipe request bodies for API tests. */
public final class RecipeJson {

    private RecipeJson() {
    }

    public static String paneerTikka() {
        return recipe("Paneer Tikka", "VEG", """
                [{"name":"Paneer","quantity":"250 g"},{"name":"  Hung   Curd ","quantity":"1/2 cup"},
                 {"name":"Bell Pepper","quantity":"1"}]""");
    }

    public static String recipe(String title, String diet, String ingredientsJson) {
        return """
                {"title":"%s","description":"Smoky grilled paneer.","dietType":"%s","cuisine":"INDIAN","course":"APPETIZER",
                 "prepMinutes":20,"cookMinutes":15,"servings":4,"cost":250.50,"calories":320,"protein":18,"difficulty":2,
                 "ingredients":%s,
                 "equipment":["Oven","Skewers","oven"],
                 "steps":["Marinate the paneer.","Thread onto skewers.","Grill for 15 minutes."]}
                """.formatted(title, diet, ingredientsJson);
    }
}
