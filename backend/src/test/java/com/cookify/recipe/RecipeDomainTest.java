package com.cookify.recipe;

import com.cookify.recipe.domain.Course;
import com.cookify.recipe.domain.Cuisine;
import com.cookify.recipe.domain.DietType;
import com.cookify.recipe.domain.EggetarianRecipe;
import com.cookify.recipe.domain.IngredientLine;
import com.cookify.recipe.domain.JainRecipe;
import com.cookify.recipe.domain.NonVegRecipe;
import com.cookify.recipe.domain.PescetarianRecipe;
import com.cookify.recipe.domain.Recipe;
import com.cookify.recipe.domain.RecipeFactory;
import com.cookify.recipe.domain.RecipeSpec;
import com.cookify.recipe.domain.VeganRecipe;
import com.cookify.recipe.domain.VegRecipe;
import com.cookify.recipe.domain.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Step 5: the Recipe class hierarchy — factory, polymorphic rules and diet compatibility. */
class RecipeDomainTest {

    @Test
    void factoryCreatesSubclassForEachDiet() {
        assertThat(recipe(DietType.VEG)).isExactlyInstanceOf(VegRecipe.class);
        assertThat(recipe(DietType.VEGAN)).isExactlyInstanceOf(VeganRecipe.class).isInstanceOf(VegRecipe.class);
        assertThat(recipe(DietType.JAIN)).isExactlyInstanceOf(JainRecipe.class).isInstanceOf(VegRecipe.class);
        assertThat(recipe(DietType.EGGETARIAN)).isExactlyInstanceOf(EggetarianRecipe.class);
        assertThat(recipe(DietType.PESCETARIAN)).isExactlyInstanceOf(PescetarianRecipe.class);
        assertThat(recipe(DietType.NON_VEG)).isExactlyInstanceOf(NonVegRecipe.class);
    }

    @Test
    void eachSubclassHasItsOwnBadge() {
        assertThat(Arrays.stream(DietType.values()).map(d -> recipe(d).getDietBadge().label()))
                .containsExactly("Vegetarian", "Vegan", "Jain", "Eggetarian", "Pescetarian", "Non-veg");
    }

    @ParameterizedTest(name = "{0} with \"{1}\" → {2}")
    @CsvSource({
            "JAIN,        red onion,          onion",
            "JAIN,        ginger garlic paste, ginger",  // earliest forbidden word is reported
            "JAIN,        potatoes,           potato",
            "JAIN,        paneer,             ",
            "JAIN,        dry ginger powder,  ",
            "VEGAN,       paneer,             paneer",
            "VEGAN,       ghee,               ghee",
            "VEGAN,       coconut milk,       ",
            "VEGAN,       peanut butter,      ",
            "VEG,         eggs,               egg",
            "VEG,         eggplant,           ",
            "VEG,         chicken stock,      chicken",
            "VEG,         paneer,             ",
            "EGGETARIAN,  boiled egg,         ",
            "EGGETARIAN,  prawns,             prawn",
            "PESCETARIAN, salmon fillet,      ",
            "PESCETARIAN, mutton,             mutton",
            "NON_VEG,     chicken thighs,     "
    })
    void dietRulesCheckIngredients(DietType diet, String ingredient, String forbiddenWord) {
        List<Violation> violations = recipe(diet, List.of(new IngredientLine(ingredient, null)), 10, 10).validate();
        if (forbiddenWord == null) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations).singleElement().satisfies(v -> {
                assertThat(v.field()).isEqualTo("ingredients");
                assertThat(v.message()).contains("cannot contain " + forbiddenWord);
            });
        }
    }

    @Test
    void subclassesInheritParentRestrictions() {
        // Vegan and Jain extend Veg, so meat is forbidden for them too (via super.forbiddenIngredients()).
        List<IngredientLine> chicken = List.of(new IngredientLine("chicken", "200 g"));
        assertThat(recipe(DietType.VEGAN, chicken, 10, 10).validate()).isNotEmpty();
        assertThat(recipe(DietType.JAIN, chicken, 10, 10).validate()).isNotEmpty();
    }

    @ParameterizedTest(name = "preference {0} can eat {1}")
    @CsvSource({
            "VEG,         'VEG,VEGAN,JAIN'",
            "VEGAN,       'VEGAN'",
            "JAIN,        'JAIN'",
            "EGGETARIAN,  'VEG,VEGAN,JAIN,EGGETARIAN'",
            "PESCETARIAN, 'VEG,VEGAN,JAIN,EGGETARIAN,PESCETARIAN'",
            "NON_VEG,     'VEG,VEGAN,JAIN,EGGETARIAN,PESCETARIAN,NON_VEG'"
    })
    void compatibilityComesFromEachSubclass(DietType preference, String expected) {
        assertThat(RecipeFactory.dietsSuitableFor(preference))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(expected.split(",")).map(DietType::valueOf).toList());
    }

    @Test
    void noPreferenceAllowsEverything() {
        assertThat(RecipeFactory.dietsSuitableFor(null)).containsExactlyInAnyOrder(DietType.values());
    }

    @ParameterizedTest
    @CsvSource({"5,10,5", "10,20,4", "20,40,3", "30,90,2", "60,120,1"})
    void speedStarsComeFromTotalTime(int prep, int cook, int stars) {
        assertThat(recipe(DietType.VEG, List.of(), prep, cook).getSpeed()).isEqualTo(stars);
    }

    @Test
    void zeroTotalTimeIsAViolation() {
        assertThat(recipe(DietType.VEG, List.of(), 0, 0).validate())
                .extracting(Violation::field).containsExactly("cookMinutes");
    }

    private static Recipe recipe(DietType diet) {
        return recipe(diet, List.of(), 10, 10);
    }

    private static Recipe recipe(DietType diet, List<IngredientLine> ingredients, int prep, int cook) {
        return RecipeFactory.create(new RecipeSpec("Test", null, diet, Cuisine.INDIAN, Course.MAIN, prep, cook, 2,
                BigDecimal.TEN, 300, 10, 2, ingredients, List.of(), List.of("Cook it")));
    }
}
