package io.github.joana_team.catshop.shop.server.impl;

import edu.kit.joana.ui.annotations.ReturnValue;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;
import io.github.joana_team.catshop.model.CatWithPersonalities;
import io.github.joana_team.catshop.model.Personality;
import io.github.joana_team.catshop.shop.server.ShopApi;
import io.github.joana_team.catshop.wiki.ApiException;
import io.github.joana_team.catshop.wiki.client.WikiApi;

import javax.ws.rs.NotAllowedException;
import java.util.*;

/**
 * shop
 *
 * <p>A shop to buy cats
 *
 */
public class ShopApiServiceImpl implements ShopApi {

    private final String PASSWORD = "PASSWORD";

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
        if (!password.equals(PASSWORD)) {
            throw new NotAllowedException("Wrong password");
        }
        speciesWithPrice.put(species, price);
    }

    /**
     * gets the cats below a certain max price that are available, with their activity level
     *
     */
    @ReturnValue(sinks = @Sink)
    public List<CatWithPersonalities> availableSpecies(@Source Integer maxPrice) {
        if (maxPrice == 1){
            return null;
        }
        List<CatWithPersonalities> cats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : speciesWithPrice.entrySet()) {
            if (entry.getValue() <= maxPrice) {
                cats.add(new CatWithPersonalities().species(entry.getKey())
                    .personalities(getPersonalities(entry.getKey())));
            }
        }
        return cats;
        /*return speciesWithPrice.entrySet().stream()
            .filter(e -> e.getValue() <= maxPrice).map(e -> new CatWithPersonalities().species(e.getKey())
                .personalities(getPersonalities(e.getKey()))).collect(
            Collectors.toList());*/
    }

    private List<Personality> getPersonalities(String species) {
        try {
            return new WikiApi().catPersonalities(species);
        } catch (ApiException e) {
            return Collections.emptyList();
        }
    }
}
