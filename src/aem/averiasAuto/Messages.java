package aem.averiasAuto;

import java.io.File;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "aem.averiasAuto.messages"; //$NON-NLS-1$

	private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	/** Cambia el idioma del frontend de la aplicación. Debe indicarse un código de idioma (por ejemplo "eus")
	 * y existir un fichero de recursos correspondiente (p ej aem.averiasAuto.messages-eus.properties).
	 * Si hay algún error, el idioma no se cambia.
	 * @param languageCode	Código de idioma
	 */
	public static void changeLanguage( String languageCode ) {
		String resFile = BUNDLE_NAME + "-" + languageCode + ".properties";
		if ((new File( resFile )).exists()) {
			try {
				RESOURCE_BUNDLE = ResourceBundle.getBundle( BUNDLE_NAME + "-" + languageCode );
			} catch (Exception e) {}  // Error en carga de recurso
		}
	}
}
