package io.github.joana_team.catshop.shop.server.impl;

import io.github.joana_team.catshop.model.CatWithPersonalities;
import io.github.joana_team.catshop.model.Personality;
import io.github.joana_team.catshop.shop.server.ShopApi;
import io.github.joana_team.catshop.wiki.ApiException;
import io.github.joana_team.catshop.wiki.client.WikiApi;

import java.util.*;

/**
 * shop
 *
 * <p>A shop to buy cats
 *
 */
public class ShopApiServiceImpl implements ShopApi {

    private final String PASSWORD = "PASSWORD";

    private String xyz = "";

    private Map<String, Integer> speciesWithPrice = new HashMap<>();
    {
        speciesWithPrice.put("norwegian", 1000);
        speciesWithPrice.put("urban", 300);
    }

    /**
     * add a species to the shop
     *
     */
    public void addAvailableSpecies(String species, Integer price, String password) {
        xyz = species;
    }

    /**
     * gets the cats below a certain max price that are available, with their activity level
     *
     */
    public List<CatWithPersonalities> availableSpecies(Integer maxPrice) {
        if (xyz.equals("asd")) {
            return Collections.emptyList();
        }
        return null;
    }

    private List<Personality> getPersonalities(String species) {
        try {
            return new WikiApi().catPersonalities(species);
        } catch (ApiException e) {
            return Collections.emptyList();
        }
    }
}
