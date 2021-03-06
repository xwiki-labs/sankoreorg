package org.curriki.xwiki.plugin.asset.attachment;

import org.curriki.xwiki.plugin.asset.Constants;
import org.curriki.xwiki.plugin.asset.Asset;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 10 d�c. 2008
 * Time: 19:43:01
 * To change this template use File | Settings | File Templates.
 */
public class InteractiveAssetManager extends AttachmentAssetManager {

    public static String CATEGORY_NAME = Constants.ASSET_CATEGORY_INTERACTIVE;
    public static  Class<? extends Asset> ASSET_CLASS = InteractiveAsset.class;

    public String getCategory() {
         return CATEGORY_NAME;
    }

    public Class<? extends Asset> getAssetClass() {
        return ASSET_CLASS;
    }
    
   
}
