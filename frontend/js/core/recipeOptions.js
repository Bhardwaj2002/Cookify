/** Option lists mirroring the backend enums (labels come from locales: diet.*, cuisine.*, course.*). */

export const DIETS = [
  { value: 'VEG', icon: '🥦' },
  { value: 'VEGAN', icon: '🌱' },
  { value: 'JAIN', icon: '🙏' },
  { value: 'EGGETARIAN', icon: '🥚' },
  { value: 'PESCETARIAN', icon: '🐟' },
  { value: 'NON_VEG', icon: '🍗' },
];

export const CUISINES = ['INDIAN', 'PAN_ASIAN', 'CHINESE', 'THAI', 'JAPANESE', 'ITALIAN', 'MEXICAN', 'ENGLISH',
  'AMERICAN', 'CONTINENTAL', 'MEDITERRANEAN', 'MIDDLE_EASTERN', 'OTHER'];

export const COURSES = ['BREAKFAST', 'APPETIZER', 'SOUP', 'SALAD', 'MAIN', 'SIDE', 'BREAD', 'RICE', 'SNACK',
  'DESSERT', 'BEVERAGE'];

/** Common utensils offered as one-click suggestions in the editor. */
export const COMMON_EQUIPMENT = ['pan', 'kadai', 'pressure cooker', 'oven', 'microwave', 'blender', 'mixer grinder',
  'tawa', 'saucepan', 'baking tray', 'whisk', 'air fryer'];
