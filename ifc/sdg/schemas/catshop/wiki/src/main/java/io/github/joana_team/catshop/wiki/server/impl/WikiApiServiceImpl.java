package io.github.joana_team.catshop.wiki.server.impl;

import edu.kit.joana.ui.annotations.ReturnValue;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;
import io.github.joana_team.catshop.model.Personality;
import io.github.joana_team.catshop.shop.ApiException;
import io.github.joana_team.catshop.shop.client.ShopApi;
import io.github.joana_team.catshop.wiki.server.WikiApi;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAllowedException;
import java.util.*;

/**
 * Wiki
 *
 * <p>A wiki for information on cats
 *
 */
public class WikiApiServiceImpl implements WikiApi {
    private final String PASSWORD = "PASSWORD";
    private Map<String, List<Personality>> personalities = new HashMap<>();
    {
        personalities.put("norwegian", Arrays.asList(Personality.WILD, Personality.ACTIVE));
        personalities.put("urban", Arrays.asList(Personality.PLAYFUL, Personality.TIDY));
    }
    private List<String> shopUrls = new ArrayList<>();
    {
        shopUrls.add("http://localhost:9010");
    }
    /**
     * personalities of a cat
     *
     */
    public List<Personality> catPersonalities(@Sink String species) {
        if (personalities.containsKey(species)) {
            return personalities.get(species);
        }
        throw new BadRequestException("Unknown species " + species);
    }

    /**
     * get shops that have the species of cats available
     *
     */
    @ReturnValue(sinks = @Sink)
    public List<String> shops(@Source String species) {
        try {
            new ShopApi().addAvailableSpecies(species, 10, PASSWORD);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        try {
            return Arrays.asList(new ShopApi().availableSpecies(10).size() + "");
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * register a cat shop
     *
     */
    public void registerShop(String shopURL) {
        shopUrls.add(shopURL);
    }

    /**
     * set the personalities of a cat species
     *
     */
    public void setCatPersonalities(String species, List<Personality> personalities, String password) {
        if (!password.equals(PASSWORD)) {
            throw new NotAllowedException("Wrong password");
        }
        this.personalities.put(species, personalities);
    }

}
