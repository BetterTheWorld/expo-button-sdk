const { ConfigPlugin, AndroidConfig, withAndroidManifest } = require('@expo/config-plugins');
const fs = require('fs');
const fsPromises = require('fs/promises');
const path = require('path');

const { Paths } = AndroidConfig;

const withTrustLocalCerts = (config) => {
    return withAndroidManifest(config, async (config) => {
        config.modResults = await setCustomConfigAsync(config, config.modResults);
        return config;
    });
};

async function setCustomConfigAsync(config, androidManifest) {
    const srcFilePath = path.join(__dirname, "network_security_config.xml");
    const resFilePath = path.join(
        await Paths.getResourceFolderAsync(config.modRequest.projectRoot),
        "xml",
        "network_security_config.xml"
    );

    const resDir = path.dirname(resFilePath);

    if (!fs.existsSync(resDir)) {
        await fsPromises.mkdir(resDir, { recursive: true });
    }

    try {
        await fsPromises.copyFile(srcFilePath, resFilePath);
    } catch (error) {
        throw new Error(`Failed to copy network_security_config.xml: ${error}`);
    }

    const mainApplication = AndroidConfig.Manifest.getMainApplicationOrThrow(androidManifest);
    mainApplication.$["android:networkSecurityConfig"] = "@xml/network_security_config";

    return androidManifest;
}

module.exports = withTrustLocalCerts;
