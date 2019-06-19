package aem.averiasAuto;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class EditorAveriasAuto {
	public static final String VERSION_PROYECTO = "Editor AutoGame 0.33"; //$NON-NLS-1$
	private static final String NOMBRE_FICHERO_LOG = "EditorAverias"; //$NON-NLS-1$
	private static final String EXT_FICHERO_LOG = ".log.xml"; //$NON-NLS-1$
	private static long MAX_SIZE_FICHERO_LOG = 50L * 1024L * 1024L;  // 50 Mb tamaño máximo fichero log para reiniciarlo
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater( new Runnable() { @Override public void run() {
			VentanaEditorAveriaAuto v = new VentanaEditorAveriaAuto();
			v.setVisible( true );
		} } );
	}

	/////////////////////////////////////////////////////////////////////
	//                      Logging                                    //
	/////////////////////////////////////////////////////////////////////
	
	public static Logger logger = initLogger();
	/** Asigna un logger ya creado para que se haga log de las operaciones del EditorAveriasAuto
	 * Si no se llama a este método, se crea un logger por defecto sobre el fichero EditorAverias.log.xml
	 * @param logger	Logger ya creado
	 */
	public static void setLogger( Logger logger ) {
		EditorAveriasAuto.logger = logger;
	}
	// Método local para loggear (si no se asigna un logger externo, se asigna uno local)
	private static Logger initLogger() {
		if (logger==null) {  // Logger por defecto local:
			// Reinicio de fichero de logger si ya muy grande
			File fLoggerAnt = new File( NOMBRE_FICHERO_LOG + EXT_FICHERO_LOG );
			if (fLoggerAnt.exists() && fLoggerAnt.length() > MAX_SIZE_FICHERO_LOG ) {
				String newFicLog = NOMBRE_FICHERO_LOG + "-" + fLoggerAnt.lastModified() + EXT_FICHERO_LOG; //$NON-NLS-1$
				try {
					Files.move( fLoggerAnt.toPath(), Paths.get(newFicLog) );  // Renombra el fichero para empezar de nuevo
				} catch (Exception e) {}
			}
			// Creación de logger asociado a fichero de logger
			logger = Logger.getLogger( EditorAveriasAuto.class.getName() );  // Nombre del logger - el de la clase
			logger.setLevel( Level.ALL );  // Loguea todos los niveles
			try {
				// logger.addHandler( new FileHandler( "editoraverias-" + System.currentTimeMillis() + ".log.xml" ) );  // Y saca el log a fichero xml
				logger.addHandler( new FileHandler( NOMBRE_FICHERO_LOG + EXT_FICHERO_LOG, true ) );  // Y saca el log a fichero xml (añadiendo al log previo)
			} catch (Exception e) {
				JOptionPane.showMessageDialog( null, Messages.getString("EditorAveriasAuto.4"),  //$NON-NLS-1$
						Messages.getString("EditorAveriasAuto.5"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$
			}
		}
		return logger;
	}
	
}



/* Teclas rápidas:
	Guardar: Ctrl+G
	Deshacer: Ctrl+Z
	Borrar: Supr
	Modo de selección: Ctrl+S
	Modo de mover: Ctrl+V
	Modo de punto: Ctrl+P
	Modo de conexión: Ctrl+C
	Modo de conector: Ctrl+T
	Inserción de componente Fusible: Ctrl+F
	Inserción de componente Lámpara: Ctrl+L
	Inserción de componente Batería: Ctrl+B
	Inserción de componente Relé: Ctrl+R
	Inserción de componente Pot.alt.luces: Ctrl+O
	Inserción de componente Motor: Ctrl+M
	Inserción de componente Interruptor: Ctrl+I
	Inserción de componente Bocina: Ctrl+A
	Salir de edición de componente: Escape
	Zoom +: Ctrl++
	Zoom -: Ctrl+-
	Scroll izquierda: Ctrl+cursor izquierda
	Scroll derecha: Ctrl+cursor derecha
	Scroll arriba: Ctrl+cursor arriba
	Scroll abajo: Ctrl+cursor abajo
*/