package org.curriki.xwiki.servlet.restlet.resource.assets;

import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.restlet.resource.ResourceException;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.curriki.xwiki.servlet.restlet.resource.BaseResource;
import org.curriki.xwiki.plugin.asset.Asset;
import org.curriki.xwiki.plugin.asset.Constants;
import net.sf.json.JSONObject;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.XWikiException;

import java.util.List;
import java.util.ArrayList;

/**
 */
public class MetadataResource extends BaseResource {
    public MetadataResource(Context context, Request request, Response response) {
        super(context, request, response);
        setReadable(true);
        setModifiable(true);
        defaultVariants();
    }

    @Override public Representation represent(Variant variant) throws ResourceException {
        setupXWiki();

        Request request = getRequest();
        String assetName = (String) request.getAttributes().get("assetName");

        List<Property> results;
        try {
            results = plugin.fetchAssetMetadata(assetName);
        } catch (XWikiException e) {
            throw error(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
        }

        JSONObject json = new JSONObject();
        for (Property prop : results) {
            json.put(prop.getName(), prop.getValue());
        }

        return formatJSON(json, variant);
    }

    @Override public void storeRepresentation(Representation representation) throws ResourceException {
        setupXWiki();

        JSONObject json = representationToJSONObject(representation);

        Request request = getRequest();
        String assetName = (String) request.getAttributes().get("assetName");

        Asset asset;
        try {
            asset = plugin.fetchAsset(assetName);
        } catch (XWikiException e) {
            throw error(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
        }

        // title
        if (json.has("title")) {
            asset.setTitle(json.getString("title"));
        } else {
            // we need to force the write mode on the asset
            // otherwise the objects retrieved won't be cloned versions
            asset.setTitle(asset.getTitle());
        }

        // We need to be carefull when interacting with assets in write mode like that
        // getObject does not retrieve the object in write mode if the asset has not been
        // switched to write mode first. We might need a function to switch to write mode
        Object assetObj = asset.getObject(Constants.ASSET_CLASS);
        Object licenseObj = asset.getObject(Constants.ASSET_LICENCE_CLASS);

        // description
        if (json.has("description")) {
            assetObj.set(Constants.ASSET_CLASS_DESCRIPTION,  json.getString("description"));
        }

        // education_system
        if (json.has(Constants.ASSET_CLASS_EDUCATION_SYSTEM)) {
            assetObj.set(Constants.ASSET_CLASS_EDUCATION_SYSTEM, json.getString(Constants.ASSET_CLASS_EDUCATION_SYSTEM));
        }

        // educational_level (array)
        List educational_level;
        if (json.has("educational_level")) {
            educational_level = json.getJSONArray("educational_level");
        } else {
            educational_level = new ArrayList();
        }
        assetObj.set(Constants.ASSET_CLASS_EDUCATIONAL_LEVEL,  educational_level);
        
        // fw_items (array)
        List fw_items;
        if (json.has("fw_items")) {
            fw_items = json.getJSONArray("fw_items");
        } else {
            fw_items = new ArrayList();
        }
        assetObj.set(Constants.ASSET_CLASS_FRAMEWORK_ITEMS,  fw_items);

        // instructional_component (array)
        List instructional_component;
        if (json.has("instructional_component")) {
            instructional_component = json.getJSONArray("instructional_component");
        } else {
            instructional_component = new ArrayList();
        }
        assetObj.set(Constants.ASSET_CLASS_INSTRUCTIONAL_COMPONENT,  instructional_component);

        // keywords
        if (json.has("keywords")) {
            assetObj.set(Constants.ASSET_CLASS_KEYWORDS,  json.getString("keywords"));
        }

        // language
        if (json.has("language")) {
            assetObj.set(Constants.ASSET_CLASS_LANGUAGE,  json.getString("language"));
        }

        // hidden_from_search ("on")
        if (json.has("hidden_from_search")) {
            assetObj.set(Constants.ASSET_CLASS_HIDDEN_FROM_SEARCH, json.getString("hidden_from_search").equals("on")?1:0);
        } else {
            assetObj.set(Constants.ASSET_CLASS_HIDDEN_FROM_SEARCH, 0);
        }

        // rating
        if (json.has("rating")) {
            long rating = json.optLong("rating");
            if (rating > 0) {
                try {
                    asset.newComment("", rating);
                } catch (XWikiException e) {
                    throw error(Status.SERVER_ERROR_INTERNAL, e.getMessage());
                }
            }
        }

        // license_deed
        if (json.has("license_type")) {
            licenseObj.set(Constants.ASSET_LICENCE_ITEM_LICENCE_TYPE,  json.getString("license_type"));
        }
        // rights_holder
        if (json.has("right_holder")) {
            licenseObj.set(Constants.ASSET_LICENCE_ITEM_RIGHTS_HOLDER,  json.getString("right_holder"));
        }

        // CURRIKI-3100
        //   Moved last as something here was making the other SRI2 items not get saved.
        //   TODO: Figure out why this was breaking things
        // rights
        if (json.has("rights")) {
            try {
                asset.applyRightsPolicy(json.getString("rights"));
            } catch (XWikiException e) {
                throw error(Status.SERVER_ERROR_INTERNAL, e.getMessage());
            }
        }

        try {
            asset.save("Set Metadata", true);
        } catch (XWikiException e) {
            throw error(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
        }

        try {
            asset.autoNominate();
        } catch (XWikiException e) {
            throw error(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
        }

        List<Property> metadata;
        try {
            metadata = plugin.fetchAssetMetadata(assetName);
        } catch (XWikiException e) {
            throw error(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
        }

        JSONObject out = new JSONObject();

        for (Property prop : metadata){
            if (json.get(prop.getName()) != null){
                out.put(prop.getName(), prop.getValue());
            }
        }
        out.put("rating", asset.getValue("rating"));

        getResponse().setEntity(formatJSON(out, getPreferredVariant()));
    }
}