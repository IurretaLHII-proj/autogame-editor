package aem.averiasAuto;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import static aem.averiasAuto.EditorAveriasAuto.logger;

import aem.averiasAuto.elem.*;

public class ControladorVentanaEAA {
	public static enum Estado { CIRCUITO, INTERRUPTOR, VOLTAJES, RESISTENCIAS };
	private static ControladorVentanaEAA lastCont = null;  // ültimo controlador creado
	
	VentanaEditorAveriaAuto v;  // Ventana asociada
	protected MarcasAverias marcasAverias;
	private Point inicioDrag;    // Para gestión de ratón
	private Point pInicioDragAbs, pDestinoDragAbs;  // Puntos de inicio y final de ratón 
	private Point pInicioCompon;
	private boolean enPuntosAdicsComponente = false;
	ArrayList<Point> puntosAdics;
	
	private DefaultTableModel modeloTablaDerecha;  // Modelo de datos de JTable
	private ArrayList<ObjetoDeGrafico> odgsEnTablaDerecha;  // ODGs en ese modelo de datos
	private boolean flagCambiandoSeleccionTabla = false;
	private ObjetoDeGrafico odgDePanelEdicionActual = null;
	
	private static boolean hayCambiosEnCircuito = false;
	
	public ControladorVentanaEAA( VentanaEditorAveriaAuto v ) {
		this.v = v;
		lastCont = this;
	}

	private Estado estado;  // Para gestión de estados de interacción
	private Estado ultEstado = null;  
	public void setEstado( Estado estado ) { if (estado!=Estado.INTERRUPTOR || this.estado!=Estado.INTERRUPTOR) ultEstado = this.estado; this.estado = estado; }		
	public Estado getEstado() { return estado; }
	public void restauraAnteriorEstado() { estado = ultEstado; }

	protected java.util.Properties miConfiguracion;
	protected static String ultimoDirectorio = ""; //$NON-NLS-1$
	public static String getUltimoDirectorio() { return ultimoDirectorio; }
	
	/** Devuelve el último controlador de ventana creado
	 * @return	Último controlador creado
	 */
	public static ControladorVentanaEAA getLast() { return lastCont; }

		// Atributos para cálculos de inicialización de tamaño de tabla
		private transient int divisorTabla = -1;
		private transient int anchCol1Tabla = -1;
		private transient int anchCol2Tabla = -1;

		
	//
	//
	// Inicialización de controlador y ventana
	//
	//
		
	public void init() {
		hayCambiosEnCircuito = false;
		// Properties
		String nomFic = ""; //$NON-NLS-1$
		miConfiguracion = new Properties();
		try {
			try { Files.createDirectory( new File( "data" ).toPath() ); } catch (IOException e1) {}
			miConfiguracion.loadFromXML( new FileInputStream( new java.io.File("data/marcasAverias.ini") ) ); //$NON-NLS-1$
			ultimoDirectorio = miConfiguracion.getProperty( "ULTIMO-DIR" ); //$NON-NLS-1$
			if (ultimoDirectorio==null) ultimoDirectorio = System.getProperty( "user.dir" ); //$NON-NLS-1$
			nomFic = miConfiguracion.getProperty( "IMAGEN" ); //$NON-NLS-1$
			int anchVent = Integer.parseInt( miConfiguracion.getProperty( "ANCHURA-VENTANA" ) ); //$NON-NLS-1$
			int altVent = Integer.parseInt( miConfiguracion.getProperty( "ALTURA-VENTANA" ) ); //$NON-NLS-1$
			v.setSize( anchVent, altVent );
			int xVent = Integer.parseInt( miConfiguracion.getProperty( "X-VENTANA" ) ); //$NON-NLS-1$
			int yVent = Integer.parseInt( miConfiguracion.getProperty( "Y-VENTANA" ) ); //$NON-NLS-1$
			v.setLocation( xVent, yVent );
			String divTab = miConfiguracion.getProperty( "ANCH-TABLA" ); //$NON-NLS-1$
			if (divTab!=null) divisorTabla = Integer.parseInt( divTab );
			divTab = miConfiguracion.getProperty( "ANCH-COL1TABLA" ); //$NON-NLS-1$
			if (divTab!=null) anchCol1Tabla = Integer.parseInt( divTab );
			divTab = miConfiguracion.getProperty( "ANCH-COL2TABLA" ); //$NON-NLS-1$
			if (divTab!=null) anchCol2Tabla = Integer.parseInt( divTab );
			actualizaMenuUltimosFicheros();
		} catch (Exception e) {
			miConfiguracion = new Properties();
			ultimoDirectorio = System.getProperty( "user.dir" ); //$NON-NLS-1$
			logger.log( Level.INFO, Messages.getString("ControladorVentanaEAA.14"), e ); //$NON-NLS-1$
		}  // No se ha encontrado el fichero de configuración
		
		// Objeto de datos principal (MarcasAverias) y panel de dibujo
		marcasAverias = new MarcasAverias( nomFic );
		v.pDibujo = new PanelDeDibujo( marcasAverias );
		marcasAverias.setPanelDibujo( v.pDibujo );
		
	}
	
	// Proceso de inicialización al final de la construcción de la ventana
	public void initFinConstruccion() {
		// Configuración componentes
		if (divisorTabla == -1)
			v.spCentral.setDividerLocation( 0.75 );
		else
			v.spCentral.setDividerLocation( divisorTabla );
		if (anchCol1Tabla == -1)
			v.tablaDerecha.getColumnModel().getColumn(0).setPreferredWidth( 10 );
		else
			v.tablaDerecha.getColumnModel().getColumn(0).setPreferredWidth( anchCol1Tabla );
		if (anchCol2Tabla == -1)
			v.tablaDerecha.getColumnModel().getColumn(1).setPreferredWidth( 10 );
		else
			v.tablaDerecha.getColumnModel().getColumn(1).setPreferredWidth( anchCol2Tabla );
		// Cargar fichero ya existente si lo hay
		String nomFicAnterior = miConfiguracion.getProperty( "ULTIMO-FIC" ); //$NON-NLS-1$
		cargarCircuito( nomFicAnterior );
	}

	public MarcasAverias getMarcasAverias() {
		return marcasAverias;
	}
	
	public ObjetoDeGrafico getODGDePanelDeEdicionActual() {
		return odgDePanelEdicionActual;
	}
	
	//
	//
	// Propiedades
	//
	//
	
	/** Devuelve el objeto de propiedades principal de la aplicación (de la ventana en curso)
	 * @return
	 */
	public static Properties getProperties() { return VentanaEditorAveriaAuto.getLastVentana().getControlador().miConfiguracion; }
	
	/** Devuelve el la propiedad indicada de la aplicación (de la ventana en curso)
	 * @return
	 */
	public static String getProperty( String prop ) { return VentanaEditorAveriaAuto.getLastVentana().getControlador().miConfiguracion.getProperty(prop); }
	
	
	//
	//
	// Gestión de botones
	//
	//
	
	public void clickBConfig() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.16") ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		v.ventConfiguracion.setVisible( true );
	}
	
	public void clickBGuardar() {
		logger.log( Level.FINEST, "Pulsación botón Guardar" );
		String nomFic = marcasAverias.guardar();
		if (nomFic!=null) {
			hayCambiosEnCircuito = false;
			actualizaUltimosFicheros( nomFic );
			actualizaTituloVentana( nomFic );
			// System.out.println( nomFic );
		}
	}
	
	public void clickBEditarAverias() {
		logger.log( Level.FINEST, "Pulsación botón Editar Averías" );
		int conf = JOptionPane.showConfirmDialog( v, 
				Messages.getString("ControladorVentanaEAA.19"),  //$NON-NLS-1$
				Messages.getString("ControladorVentanaEAA.20"), JOptionPane.YES_NO_OPTION ); //$NON-NLS-1$
		if (conf==0) {
			if (listoParaAverias()) {
				hayCambiosEnCircuito = true;
				Acciones.getInstance().reset( marcasAverias );   // Reinicia el sistema de acciones -no se puede deshacer lo anterior-
				marcasAverias.setModoEdicion( false );
				setModoEdicion( false );
			} else {
				JOptionPane.showMessageDialog( null, Messages.getString("ControladorVentanaEAA.21") +  //$NON-NLS-1$
						Messages.getString("ControladorVentanaEAA.22"),  //$NON-NLS-1$
						Messages.getString("ControladorVentanaEAA.23"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$
			}
		}
	}

	public void clickBRecalcInterrupcion() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.24") ); //$NON-NLS-1$
		int conf = JOptionPane.showConfirmDialog( v, 
				Messages.getString("ControladorVentanaEAA.25"),  //$NON-NLS-1$
				Messages.getString("ControladorVentanaEAA.26"), JOptionPane.YES_NO_OPTION ); //$NON-NLS-1$
		if (conf==0) {
			hayCambiosEnCircuito = true;
			marcasAverias.setModoEdicion( false );
			//  setModoEdicion( false );   // Ya está a false, no hace falta
		}
	}
		
	/** Identifica todos los puntos no conectados con ningún componente, y los marca como no conectables
	 */
	public void clickBQuitaConectables() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.27") ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		ArrayList<Punto> puntosNoEsenciales = new ArrayList<>( marcasAverias.getPuntos() );
		for (Componente c : marcasAverias.getComponentes()) {
			for (Punto p : c.getAL()) {
				puntosNoEsenciales.remove( p );
			}
		}
		for (Punto p : puntosNoEsenciales) p.setConectable( false );
		redibujaCircuito();
	}

	public void clickBPreCalculo() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.28") ); //$NON-NLS-1$
		int conf = JOptionPane.showConfirmDialog( v, 
				Messages.getString("ControladorVentanaEAA.29"),  //$NON-NLS-1$
				Messages.getString("ControladorVentanaEAA.30"), JOptionPane.YES_NO_OPTION ); //$NON-NLS-1$
		if (conf==0) {
			hayCambiosEnCircuito = true;
			marcasAverias.recalculaTensiones();
			marcasAverias.recalculaResistencias();
			redibujaCircuito();
		}
	}
	
	public void clickBEdiResistencias() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.31") ); //$NON-NLS-1$
		if (getEstado()==Estado.RESISTENCIAS || getEstado()==Estado.VOLTAJES) {
			v.setMensaje( " " ); //$NON-NLS-1$
			if (getEstado()==Estado.RESISTENCIAS) {
				setModoResistencias( false );
			} else {
				setModoResistencias( true );
			}
		}
	}

	public void clickBV0() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.33") ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		setVoltajesPuntosSel( 0.0 );
		redibujaCircuito();
	}

	public void clickBVPNC() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.34") ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		setVoltajesPuntosSel( Punto.VOLTAJE_PNC );
		redibujaCircuito();
	}

	public void clickBVInterm() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.35") ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		setVoltajesPuntosSel( Punto.VOLTAJE_INTERMITENTE1000 );
		redibujaCircuito();
	}

	public void clickBVVariable() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.36") ); //$NON-NLS-1$
		try {
			double d = Double.parseDouble( v.tfVVariable.getText() );
			hayCambiosEnCircuito = true;
			setVoltajesPuntosSel( d );
			redibujaCircuito();
		} catch (NumberFormatException ex) {
			v.setMensaje( Messages.getString("ControladorVentanaEAA.37") + v.tfVVariable.getText() + Messages.getString("ControladorVentanaEAA.38") ); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public void clickBV12() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.39") ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		setVoltajesPuntosSel( 12.0 );
		redibujaCircuito();
	}
	
	public void clickBRInf() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.40") ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		v.bR0.setSelected( false );
		v.bRVariable.setSelected( false );
		if (v.bSeleccion.isSelected()) {
			v.bRInf.setSelected( true );
			for (ObjetoDeGrafico odg : marcasAverias.getElementosSeleccionados())
				if (odg instanceof Resistencia) {
					Resistencia r = (Resistencia) odg;
					setResistencias( r );
				}
		} else {
			if (v.bRInf.isSelected()) v.setMensaje( Messages.getString("ControladorVentanaEAA.41") ); else v.setMensaje( " " ); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public void clickBRVariable() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.43") ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		v.bR0.setSelected( false );
		v.bRInf.setSelected( false );
		if (v.bSeleccion.isSelected()) {
			v.bRVariable.setSelected( true );
			for (ObjetoDeGrafico odg : marcasAverias.getElementosSeleccionados())
				if (odg instanceof Resistencia) {
					Resistencia r = (Resistencia) odg;
					setResistencias( r );
				}
		} else {
			if (v.bRVariable.isSelected()) v.setMensaje( Messages.getString("ControladorVentanaEAA.44") ); else v.setMensaje( " " ); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public void clickBR0() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.46") ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		v.bRInf.setSelected( false );
		v.bRVariable.setSelected( false );
		if (v.bSeleccion.isSelected()) {
			v.bR0.setSelected( true );
			for (ObjetoDeGrafico odg : marcasAverias.getElementosSeleccionados())
				if (odg instanceof Resistencia) {
					Resistencia r = (Resistencia) odg;
					setResistencias( r );
				}
		} else {
			if (v.bR0.isSelected()) v.setMensaje( Messages.getString("ControladorVentanaEAA.47") ); else v.setMensaje( " " ); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void clickBCargar() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.49") ); //$NON-NLS-1$
		if (confirmarGuardado()==2) return;  // Se guarda y se carga salvo que se cancele
		if (marcasAverias.cargar()) {
			hayCambiosEnCircuito = false;
			redibujaCircuito();
			cargarTabla();
			String fichero = marcasAverias.getPath();
			if (fichero!=null && !fichero.equals("")) { //$NON-NLS-1$
				actualizaUltimosFicheros( fichero );
				actualizaTituloVentana( fichero );
			}
		}
	}
	
		private void cargarCircuito( String fichero ) {
			if (confirmarGuardado()==2) return;  // Se guarda y se carga salvo que se cancele
			if (fichero!=null && !fichero.isEmpty()) {
				File f = new File( fichero );
				if (f.exists()) {
					actualizaTituloVentana( f.getName() );
					marcasAverias.cargar( fichero );
					hayCambiosEnCircuito = false;
					redibujaCircuito();
					cargarTabla();
					actualizaUltimosFicheros( fichero );
					actualizaTituloVentana( fichero );
				}
			}
		}
		
		// Pregunta interactivamente si se quiere guardar
		// Devuelve 0 para confirmar que se sigue adelante, bien se haya guardado o bien no hiciera falta (si no ha habido cambios, ni se pregunta). 
		// Devuelve 1 para confirmar que no se ha guardado pero que se siga
		// Devuelve 2 si se quiere cancelar
		private int confirmarGuardado() {
			if (hayCambiosEnCircuito) {
				int ret = JOptionPane.showConfirmDialog( v, 
						Messages.getString("ControladorVentanaEAA.51"),  //$NON-NLS-1$  ¡Atención\! Ha habido cambios en el circuito actual. ¿Quieres guardarlos?
						Messages.getString("ControladorVentanaEAA.52"), JOptionPane.YES_NO_CANCEL_OPTION ); //$NON-NLS-1$
				if (ret==0) {
					String ultimoFich = ControladorVentanaEAA.getProperty( "ULTIMO-FIC" ); //$NON-NLS-1$
					if (ultimoFich!=null && !ultimoFich.equals("")) { //$NON-NLS-1$
						marcasAverias.guardar( ultimoFich );
						hayCambiosEnCircuito = false;
					} else {
						clickBGuardar();
					}
				}
				return (ret==-1) ? 2 : ret;  // si se sale cerrando el dialogo se devuelve "cancelar"
			} else {
				return 0;
			}
		}
		

	public void clickBNuevo() {
		if (confirmarGuardado()==2) return;
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.53") ); //$NON-NLS-1$
		marcasAverias.reiniciar();
		miConfiguracion.setProperty( "ULTIMO-FIC", "" ); //$NON-NLS-1$ //$NON-NLS-2$
		v.setTitle( EditorAveriasAuto.VERSION_PROYECTO + Messages.getString("ControladorVentanaEAA.0") ); //$NON-NLS-1$
		reiniciarConfiguracion();
		cargarTabla();
		hayCambiosEnCircuito = false;
	}

	public void clickBCalcCortes() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.57") ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		marcasAverias.calcCortes();
	}

	public void clickBCargarGraf() {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.58") ); //$NON-NLS-1$
		File f = pedirFicheroImagen( Messages.getString("ControladorVentanaEAA.59") ); //$NON-NLS-1$
		if (f!=null) {
			hayCambiosEnCircuito = true;
			if (marcasAverias.getPuntos().size()>0) {  // Si ya hay puntos preguntar si reiniciar
				int conf = JOptionPane.showConfirmDialog( v, 
						Messages.getString("ControladorVentanaEAA.60"),  //$NON-NLS-1$
						Messages.getString("ControladorVentanaEAA.61"), JOptionPane.YES_NO_OPTION ); //$NON-NLS-1$
				if (conf==0) {
					marcasAverias.guardar( "ultimoCircuito.autoG" ); //$NON-NLS-1$
					marcasAverias.borraTodo();
				}
			}
			marcasAverias.nomFicheroCircuito = f.getAbsolutePath();
			v.pDibujo.setFondoImagen( marcasAverias.nomFicheroCircuito );
			String ultimoFich = miConfiguracion.getProperty( "ULTIMO-FIC" ); //$NON-NLS-1$
			if (ultimoFich==null || ultimoFich.equals("")) { //$NON-NLS-1$
				String nombreGraf = f.getName();
				int extension = nombreGraf.lastIndexOf("."); //$NON-NLS-1$
				if (extension>0) nombreGraf = nombreGraf.substring(0,extension);
				nombreGraf += ".autoG"; //$NON-NLS-1$
				String ultimoDir = miConfiguracion.getProperty( "ULTIMO-DIR" ); //$NON-NLS-1$
				if (ultimoDir==null) ultimoDir = ""; else ultimoDir += "/"; //$NON-NLS-1$ //$NON-NLS-2$
				String nuevoNombre = ultimoDir + nombreGraf;
				actualizaUltimosFicheros( nuevoNombre );
				v.setTitle( EditorAveriasAuto.VERSION_PROYECTO + " - " + nombreGraf ); //$NON-NLS-1$
			}
		}
	}

	public void clickBBorrar() {
		logger.log( Level.FINEST, "Pulsación botón Borrar" ); //$NON-NLS-1$
		hayCambiosEnCircuito = true;
		borrarSeleccion();
	}

	//
	//
	// Configuración
	//
	//
	
	public void clickLastFile( int numLast ) {
		logger.log( Level.FINEST, "Pulsación menú lastFile " + numLast ); //$NON-NLS-1$
		String property = "ULTIMO-FIC"; //$NON-NLS-1$
		if (numLast>1) property += numLast;
		String uf = miConfiguracion.getProperty( property );
		if (uf==null || uf.isEmpty()) return;
		cargarCircuito( uf );
	}
	
	private void actualizaUltimosFicheros( String nomFic ) {
		String uf1 = miConfiguracion.getProperty( "ULTIMO-FIC" ); //$NON-NLS-1$
		String uf2 = miConfiguracion.getProperty( "ULTIMO-FIC2" ); //$NON-NLS-1$
		String uf3 = miConfiguracion.getProperty( "ULTIMO-FIC3" ); //$NON-NLS-1$
		String uf4 = miConfiguracion.getProperty( "ULTIMO-FIC4" ); //$NON-NLS-1$
		if (uf1==null || !uf1.equals(nomFic)) {
			miConfiguracion.setProperty( "ULTIMO-FIC", nomFic ); //$NON-NLS-1$
			if (uf1!=null) miConfiguracion.setProperty( "ULTIMO-FIC2", uf1 ); //$NON-NLS-1$
			if (uf2==null || !uf2.equals(nomFic)) {
				if (uf2!=null) miConfiguracion.setProperty( "ULTIMO-FIC3", uf2 ); //$NON-NLS-1$
				if (uf3==null || !uf3.equals(nomFic)) {
					if (uf3!=null) miConfiguracion.setProperty( "ULTIMO-FIC4", uf3 ); //$NON-NLS-1$
					if (uf4==null || !uf4.equals(nomFic)) {
						if (uf4!=null) miConfiguracion.setProperty( "ULTIMO-FIC5", uf4 ); //$NON-NLS-1$
					}
				}
			}
		}
		actualizaMenuUltimosFicheros();
	}

	public void actualizaMenuUltimosFicheros() {
		if (v.miLastFile1!=null) {  // Si la ventana ha sido inicializada
			String uf1 = miConfiguracion.getProperty( "ULTIMO-FIC" ); //$NON-NLS-1$
			String uf2 = miConfiguracion.getProperty( "ULTIMO-FIC2" ); //$NON-NLS-1$
			String uf3 = miConfiguracion.getProperty( "ULTIMO-FIC3" ); //$NON-NLS-1$
			String uf4 = miConfiguracion.getProperty( "ULTIMO-FIC4" ); //$NON-NLS-1$
			String uf5 = miConfiguracion.getProperty( "ULTIMO-FIC5" ); //$NON-NLS-1$
			if (uf1==null || uf1.isEmpty()) { v.miLastFile1.setText( "" ); v.miLastFile1.setEnabled( false ); }  //$NON-NLS-1$
				else { v.miLastFile1.setText( uf1 ); v.miLastFile1.setEnabled( true ); }
			if (uf2==null || uf2.isEmpty()) { v.miLastFile2.setText( "" ); v.miLastFile2.setEnabled( false ); }  //$NON-NLS-1$
				else { v.miLastFile2.setText( uf2 ); v.miLastFile2.setEnabled( true ); }
			if (uf3==null || uf3.isEmpty()) { v.miLastFile3.setText( "" ); v.miLastFile3.setEnabled( false ); }  //$NON-NLS-1$
				else { v.miLastFile3.setText( uf3 ); v.miLastFile3.setEnabled( true ); }
			if (uf4==null || uf4.isEmpty()) { v.miLastFile4.setText( "" ); v.miLastFile4.setEnabled( false ); }  //$NON-NLS-1$
				else { v.miLastFile4.setText( uf4 ); v.miLastFile4.setEnabled( true ); }
			if (uf5==null || uf5.isEmpty()) { v.miLastFile5.setText( "" ); v.miLastFile5.setEnabled( false ); }  //$NON-NLS-1$
				else { v.miLastFile5.setText( uf5 ); v.miLastFile5.setEnabled( true ); }
		}
	}

	private void actualizaTituloVentana( String nomFic ) {
		String fic = Paths.get( nomFic ).getFileName().toString();
		v.setTitle( EditorAveriasAuto.VERSION_PROYECTO + " - " + fic ); //$NON-NLS-1$
	}
	
	//
	//
	// Gestión de Combos
	//
	//
	
	public void cbComponentesItem() {
		logger.log( Level.FINEST, "Pulsación combo Componente" ); //$NON-NLS-1$
		v.bComponente.setSelected( true );
	}
	public void cbTiposElementosItem() {
		logger.log( Level.FINEST, "Pulsación combo TiposElementos " ); //$NON-NLS-1$
		cargarTabla();  // Cambia la selección de la tabla de la derecha
	}
	public void lCombinacionesInterrupcionItem() {
		logger.log( Level.FINEST, "Pulsación combo Combinaciones Interrupción " + v.lCombinacionesInterrupcion.getSelectedIndex() ); //$NON-NLS-1$
		cambioCombInterrupcion();
	}

	
	//
	//
	// Escuchador de ventana - cierre 
	//
	//

	public void windowClosing() {
		logger.log( Level.FINEST, "Cerrando ventana" ); //$NON-NLS-1$
		int conf = confirmarGuardado();  // Se confirma el guardado y si es sí lo guarda
		if (conf==0 || conf==1) {  // Confirmar guardado sí o no (sin cancelar)
			// if (miConfiguracion==null) miConfiguracion = new Properties();
			try {
				miConfiguracion.setProperty( "IMAGEN", marcasAverias.nomFicheroCircuito ); //$NON-NLS-1$
				miConfiguracion.setProperty( "ANCHURA-VENTANA", "" + v.getWidth() ); //$NON-NLS-1$ //$NON-NLS-2$
				miConfiguracion.setProperty( "ALTURA-VENTANA", "" + v.getHeight() ); //$NON-NLS-1$ //$NON-NLS-2$
				miConfiguracion.setProperty( "X-VENTANA", "" + v.getLocation().x ); //$NON-NLS-1$ //$NON-NLS-2$
				miConfiguracion.setProperty( "Y-VENTANA", "" + v.getLocation().y ); //$NON-NLS-1$ //$NON-NLS-2$
				miConfiguracion.setProperty( "ANCH-TABLA", "" + v.spCentral.getDividerLocation() ); //$NON-NLS-1$ //$NON-NLS-2$
				miConfiguracion.setProperty( "ANCH-COL1TABLA", "" + v.tablaDerecha.getColumnModel().getColumn(0).getWidth() ); //$NON-NLS-1$ //$NON-NLS-2$
				miConfiguracion.setProperty( "ANCH-COL2TABLA", "" + v.tablaDerecha.getColumnModel().getColumn(1).getWidth() ); //$NON-NLS-1$ //$NON-NLS-2$
				miConfiguracion.setProperty( "ULTIMO-DIR", ultimoDirectorio ); //$NON-NLS-1$
				try { Files.createDirectory( new File( "data" ).toPath() ); } catch (IOException e1) {}
				miConfiguracion.storeToXML( new FileOutputStream( new java.io.File("data/marcasAverias.ini") ), "Configuracion editor circuitos" ); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (Exception ex) {  // No se ha podido guardar el fichero de configuración
				logger.log( Level.WARNING, "No se ha podido guardar el fichero de configuración", ex ); //$NON-NLS-1$
			}
			v.ventConfiguracion.dispose();
			v.dispose();
		}
	}

	
	//
	//
	// Escuchadores de teclado
	//
	//

		private transient boolean ctrlShiftPulsado = false;
	public KeyAdapterEAA escTecladoTextos = new KeyAdapterEAA(); 
	public class KeyAdapterEAA extends KeyAdapter {
		private boolean ctrlPulsado = false;
		private boolean shiftPulsado = false;
		public boolean isCtrlPulsado() { return ctrlPulsado; }
		public boolean isShiftPulsado() { return shiftPulsado; }
		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode()==KeyEvent.VK_CONTROL) {
				ctrlPulsado = false;
			} else if (e.getKeyCode()==KeyEvent.VK_SHIFT) {
				shiftPulsado = false;
			}
			if (!ctrlPulsado || !shiftPulsado) {
				if (ctrlShiftPulsado) {
					marcasAverias.mostrarCircuitoYTextos();
					ctrlShiftPulsado = false;
				}
			}
		}
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode()==KeyEvent.VK_CONTROL) {
				ctrlPulsado = true;
			} else if (e.getKeyCode()==KeyEvent.VK_SHIFT) {
				shiftPulsado = true;
			}
			if (ctrlPulsado && shiftPulsado) {
				logger.log( Level.FINEST, "Pulsación Tecla Ctrl+Shift " ); //$NON-NLS-1$
				marcasAverias.ocultarCircuitoYTextos();
				ctrlShiftPulsado = true;
			}
		}
	};

	public KeyListenerTeclado escTeclado = new KeyListenerTeclado();
	public class KeyListenerTeclado extends KeyAdapter {
		boolean ctrlPulsado = false;
		ArrayList<Integer> teclasNoGestionadas = new ArrayList<Integer>();
		@Override
		public Object clone() throws CloneNotSupportedException {
			KeyListenerTeclado klt2 = new KeyListenerTeclado();
			klt2.ctrlPulsado = ctrlPulsado;
			klt2.teclasNoGestionadas.addAll( teclasNoGestionadas );
			return klt2;
		}
		public void addTeclaNoGestionada( Integer keyCode ) {
			teclasNoGestionadas.add( keyCode );
		}
		public void resetTeclasNoGestionadas() {
			teclasNoGestionadas.clear();
		}
		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode()==KeyEvent.VK_CONTROL) {
				ctrlPulsado = false;
			} else {
			}
		}
		private static final int MAS_TECNUM = 107;
		private static final int MENOS_TECNUM = 109;
		@Override
		public void keyPressed(KeyEvent e) {
			if ( teclasNoGestionadas.contains(e.getKeyCode()) ) return;  // Si la tecla no se gestiona, se ignora
			if (e.getKeyCode()==KeyEvent.VK_CONTROL) {
				ctrlPulsado = true;
			} else if ((e.getKeyCode()==KeyEvent.VK_PLUS || e.getKeyCode()==MAS_TECNUM) && ctrlPulsado) {  // Ctrl++
				logger.log( Level.FINEST, "Pulsación tecla Ctrl++" ); //$NON-NLS-1$
				int valZoom = v.pDibujo.getZoomEntero()+10;
				if (valZoom>1024) valZoom = 1024;
				v.pDibujo.setZoom( valZoom );
				redibujaCircuito();
				v.slZoom.setValue( valZoom );
			} else if ((e.getKeyCode()==KeyEvent.VK_MINUS || e.getKeyCode()==MENOS_TECNUM) && ctrlPulsado) {  // Ctrl+-
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+-" ); //$NON-NLS-1$
				int valZoom = v.pDibujo.getZoomEntero()-10;
				if (valZoom<0) valZoom = 0;
				v.pDibujo.setZoom( valZoom );
				redibujaCircuito();
				v.slZoom.setValue( valZoom );
			} else if ((e.getKeyCode()==KeyEvent.VK_RIGHT) && ctrlPulsado) {  // Ctrl + ->
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+->" ); //$NON-NLS-1$
				v.spDibujo.getHorizontalScrollBar().setValue( v.spDibujo.getHorizontalScrollBar().getValue() + 15);
				redibujaCircuito();
			} else if ((e.getKeyCode()==KeyEvent.VK_LEFT) && ctrlPulsado) {  // Ctrl + <-
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+<-" ); //$NON-NLS-1$
				v.spDibujo.getHorizontalScrollBar().setValue( v.spDibujo.getHorizontalScrollBar().getValue() - 15);
				redibujaCircuito();
			} else if ((e.getKeyCode()==KeyEvent.VK_UP) && ctrlPulsado) {  // Ctrl + ^
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+^" ); //$NON-NLS-1$
				v.spDibujo.getVerticalScrollBar().setValue( v.spDibujo.getVerticalScrollBar().getValue() - 15);
				redibujaCircuito();
			} else if ((e.getKeyCode()==KeyEvent.VK_DOWN) && ctrlPulsado) {  // Ctrl + v
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+v" ); //$NON-NLS-1$
				v.spDibujo.getVerticalScrollBar().setValue( v.spDibujo.getVerticalScrollBar().getValue() + 15);
				redibujaCircuito();
			} else if (e.getKeyCode()==KeyEvent.VK_P && ctrlPulsado) {  // Ctrl+P
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+P" ); //$NON-NLS-1$
				v.bPunto.setSelected( true );
			} else if (e.getKeyCode()==KeyEvent.VK_C && ctrlPulsado) {  // Ctrl+C
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+C" ); //$NON-NLS-1$
				v.bConexion.setSelected( true );
			} else if (e.getKeyCode()==KeyEvent.VK_T && ctrlPulsado) {  // Ctrl+T
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+T" ); //$NON-NLS-1$
				v.bConector.setSelected( true );
			} else if (e.getKeyCode()==KeyEvent.VK_S && ctrlPulsado) {  // Ctrl+S
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+S" ); //$NON-NLS-1$
				if (estado==Estado.RESISTENCIAS) {
					v.bSeleccion.setSelected( v.bSeleccion.isSelected() );
				} else if (estado!=Estado.VOLTAJES)  // En voltajes se mantiene siempre seleccionado
					v.bSeleccion.setSelected( true );
			} else if (e.getKeyCode()==KeyEvent.VK_V && ctrlPulsado) {  // Ctrl+V
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+V" ); //$NON-NLS-1$
				v.bMover.setSelected( true );
			} else if (e.getKeyCode()==KeyEvent.VK_F && ctrlPulsado) {  // Ctrl+F
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+F" ); //$NON-NLS-1$
				v.bComponente.setSelected( true );
				v.cbComponentes.setSelectedItem( "Fusible" ); //$NON-NLS-1$
			} else if (e.getKeyCode()==KeyEvent.VK_L && ctrlPulsado) {  // Ctrl+L
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+L" ); //$NON-NLS-1$
				v.bComponente.setSelected( true );
				v.cbComponentes.setSelectedItem( "Lámpara" ); //$NON-NLS-1$
			} else if (e.getKeyCode()==KeyEvent.VK_B && ctrlPulsado) {  // Ctrl+B
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+B" ); //$NON-NLS-1$
				v.bComponente.setSelected( true );
				v.cbComponentes.setSelectedItem( "Batería" ); //$NON-NLS-1$
			} else if (e.getKeyCode()==KeyEvent.VK_R && ctrlPulsado) {  // Ctrl+R
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+R" ); //$NON-NLS-1$
				if (getEstado()==Estado.CIRCUITO) {
					v.bComponente.setSelected( true );
					v.cbComponentes.setSelectedItem( "Relé4" ); //$NON-NLS-1$
				} else {
					clickBEdiResistencias();
				}
			} else if (e.getKeyCode()==KeyEvent.VK_O && ctrlPulsado) {  // Ctrl+O
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+O" ); //$NON-NLS-1$
				v.bComponente.setSelected( true );
				v.cbComponentes.setSelectedItem( "Pot.alt.luces" ); //$NON-NLS-1$
			} else if (e.getKeyCode()==KeyEvent.VK_M && ctrlPulsado) {  // Ctrl+M
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+M" ); //$NON-NLS-1$
				v.bComponente.setSelected( true );
				v.cbComponentes.setSelectedItem( "Motor" ); //$NON-NLS-1$
			} else if (e.getKeyCode()==KeyEvent.VK_I && ctrlPulsado) {  // Ctrl+I
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+I" ); //$NON-NLS-1$
				v.bComponente.setSelected( true );
				v.cbComponentes.setSelectedItem( "Int2" ); //$NON-NLS-1$
			} else if (e.getKeyCode()==KeyEvent.VK_A && ctrlPulsado) {  // Ctrl+A
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+A" ); //$NON-NLS-1$
				v.bComponente.setSelected( true );
				v.cbComponentes.setSelectedItem( "Bocina" ); //$NON-NLS-1$
			} else if (e.getKeyCode()==KeyEvent.VK_G && ctrlPulsado) {  // Ctrl+G
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+G" ); //$NON-NLS-1$
				v.bGuardar.doClick();
			} else if (e.getKeyCode()==KeyEvent.VK_Z && ctrlPulsado) {  // Ctrl+Z
				logger.log( Level.FINEST, "Pulsación tecla Ctrl+Z" ); //$NON-NLS-1$
				Acciones.getInstance().deshacer();
			} else if (e.getKeyCode()==KeyEvent.VK_DELETE && !ctrlPulsado) {  // Supr
				logger.log( Level.FINEST, "Pulsación tecla Supr" ); //$NON-NLS-1$
				if (v.bBorrar.isEnabled()) v.bBorrar.doClick();
			} else if (e.getKeyCode()==KeyEvent.VK_ESCAPE && !ctrlPulsado) {  // Esc  -  sale de la edición de componente
				logger.log( Level.FINEST, "Pulsación tecla Esc" ); //$NON-NLS-1$
				if (enPuntosAdicsComponente) finEdComponente();
				else if (ControladorVentanaEAA.getLast().getEstado() == ControladorVentanaEAA.Estado.INTERRUPTOR) {
					ControladorVentanaEAA.getLast().restauraAnteriorEstado();
					VentanaEditorAveriaAuto.getLastVentana().setMensaje( " " ); //$NON-NLS-1$
					VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().getPanelDibujo().repaint();
				} else if (VentanaEditorAveriaAuto.getLastVentana().getFocusOwner() instanceof TextoDeCircuito) {
					VentanaEditorAveriaAuto.getLastVentana().bGuardar.requestFocus();
				}
			}
		}
	};
	
	//
	//
	// Interacción de ratón
	//
	//
	
	public void mouseDragged(MouseEvent e) {
		Point pEscalado = new Point( (int)Math.round(e.getPoint().x/v.pDibujo.getZoom()), (int)Math.round(e.getPoint().y/v.pDibujo.getZoom()) );
		logger.log( Level.FINEST, "MouseDragged " + e.getPoint() + " - Escalado " + pEscalado ); //$NON-NLS-1$ //$NON-NLS-2$
		if (v.bSeleccion.isSelected() || 
			(estado==Estado.RESISTENCIAS && !(pInicioDragAbs instanceof Punto)) || (estado==Estado.VOLTAJES) ||
			(estado==Estado.CIRCUITO && v.bComponente.isSelected() && "Elec".equals(v.cbComponentes.getSelectedItem()))) { //$NON-NLS-1$
			marcaRectDrag( pEscalado );
		} else {
			marcaLineaDrag( pEscalado );
		}
	}

	void mouseReleased( MouseEvent e ) {
		marcasAverias.clearMarcas(); redibujaCircuito(); // Borrar marcas de ratón
		Point pDestino = e.getPoint();
		pDestinoDragAbs = new Point( (int)Math.round(pDestino.x/v.pDibujo.getZoom()), (int)Math.round(pDestino.y/v.pDibujo.getZoom()) );
		logger.log( Level.FINEST, "MouseReleased " + e.getPoint() + " - Escalado " + pDestinoDragAbs ); //$NON-NLS-1$ //$NON-NLS-2$
		if (estado==Estado.INTERRUPTOR) {  // Si es interruptor
			boolean losDosPuntosEstaban = false;
			if (marcasAverias.existePunto(pDestinoDragAbs, v.pDibujo.getZoom()) instanceof Punto) { // Ya existe punto destino
				pDestinoDragAbs = marcasAverias.existePunto(pDestinoDragAbs, v.pDibujo.getZoom());
				losDosPuntosEstaban = true;
			} else if (e.isControlDown() && marcasAverias.existePunto(pDestinoDragAbs,MarcasAverias.CERCANIA_PUNTOS_MAYOR, v.pDibujo.getZoom()) instanceof Punto) { // Ya existe punto destino con menor precisión
				pDestinoDragAbs = marcasAverias.existePunto(pDestinoDragAbs,MarcasAverias.CERCANIA_PUNTOS_MAYOR, v.pDibujo.getZoom()); 
				losDosPuntosEstaban = true;
			}
			if (losDosPuntosEstaban && pInicioDragAbs instanceof Punto) {  // Ya existe punto inicio y final - específico para conexión de interruptores
				ObjetoDeGrafico odg = marcasAverias.getElementoSeleccionado();
				if (odg instanceof Componente) {
					hayCambiosEnCircuito = true;
					((Componente) odg).setConexionInterruptor( (Punto) pInicioDragAbs, (Punto) pDestinoDragAbs );
				}
			}
		} else if (estado==Estado.RESISTENCIAS) {  // Si se están editando resistencias
			if (v.bSeleccion.isSelected()) {  // Modo selección
				if (e.getPoint().equals(inicioDrag)) {
					if (v.bSeleccion.isSelected()) seleccionarObjetosPunto( false );
				} else {
					if (v.bSeleccion.isSelected()) seleccionarObjetosDrag( e.isControlDown() );
				}
			} else if (v.bR0.isSelected() || v.bRInf.isSelected() || v.bRVariable.isSelected()) {  // Y hay una edición concreta seleccionada
				if (marcasAverias.existePunto(pDestinoDragAbs, v.pDibujo.getZoom()) instanceof Punto) { // Ya existe punto destino
					pDestinoDragAbs = marcasAverias.existePunto(pDestinoDragAbs, v.pDibujo.getZoom());
				} else if (e.isControlDown() && marcasAverias.existePunto(pDestinoDragAbs, MarcasAverias.CERCANIA_PUNTOS_MAYOR, v.pDibujo.getZoom()) instanceof Punto) { // Ya existe punto destino con menor precisión
					pDestinoDragAbs = marcasAverias.existePunto(pDestinoDragAbs, MarcasAverias.CERCANIA_PUNTOS_MAYOR, v.pDibujo.getZoom()); 
				}
				if (pInicioDragAbs!=null && pDestinoDragAbs!=null && pInicioDragAbs instanceof Punto && pDestinoDragAbs instanceof Punto && pInicioDragAbs!=pDestinoDragAbs) {  // Inicio y destino son puntos - crear resistencia entre ellos
					hayCambiosEnCircuito = true;
					Resistencia nuevaR = marcasAverias.yaExisteResistencia( (Punto)pInicioDragAbs, (Punto)pDestinoDragAbs );
					if (nuevaR==null) {  // La resistencia no existe
						Acciones.getInstance().hacer( "Añadir resistencia" ); //$NON-NLS-1$
						nuevaR = marcasAverias.addResistencia( (Punto)pInicioDragAbs, (Punto)pDestinoDragAbs );
						nuevaR.initResistencias( marcasAverias.getCombsInterrupcion().size() );
					}
					setResistencias( nuevaR );
					refrescarTablaCon( nuevaR );
					marcasAverias.seleccionarElemento( nuevaR, false );
					// redibujaCircuito();
				} else if (pInicioDragAbs!=null && pDestinoDragAbs!=null) {  // Si no son puntos, mirar si hay elementos dentro del rectángulo y asignar resistencias a todos
					hayCambiosEnCircuito = true;
					int xIni = Math.min( pInicioDragAbs.x, pDestinoDragAbs.x );
					int yIni = Math.min( pInicioDragAbs.y, pDestinoDragAbs.y );
					Rectangle2D r = new Rectangle( xIni, yIni, Math.abs( pDestinoDragAbs.x-pInicioDragAbs.x )+1, Math.abs( pDestinoDragAbs.y-pInicioDragAbs.y )+1 ); 
					marcasAverias.seleccionarElemento( null, false );  // Quita la selección que hubiera
					for (Conexion c : marcasAverias.getConexiones()) {  // Conexiones incluidas en la selección
						Punto pIni = c.getAL().get(0);
						for (int indFin=1; indFin<c.getAL().size(); indFin++) {
							Punto pFin = c.getAL().get(indFin);
							if (r.contains( pIni ) && r.contains( pFin )) {
								Resistencia nuevaR = marcasAverias.yaExisteResistencia( pIni, pFin );
								if (nuevaR==null) {  // La resistencia no existe
									Acciones.getInstance().hacer( "Añadir resistencia" ); //$NON-NLS-1$
									nuevaR = marcasAverias.addResistencia( pIni, pFin );
									nuevaR.initResistencias( marcasAverias.getCombsInterrupcion().size() );
								}
								setResistencias( nuevaR );
								marcasAverias.seleccionarElemento( nuevaR, true );
								refrescarTablaCon( nuevaR );
							}
							pIni = pFin;
						}
					}
					for (Componente c : marcasAverias.getComponentes()) {  // Componentes de 2 terminales incluidas en la selección
						if (c.getAL().size()==2) {
							Punto pIni = c.getAL().get(0);
							Punto pFin = c.getAL().get(1);
							if (r.contains( pIni ) && r.contains( pFin )) {
								Resistencia nuevaR = marcasAverias.yaExisteResistencia( pIni, pFin );
								if (nuevaR==null) {  // La resistencia no existe
									Acciones.getInstance().hacer( "Añadir resistencia" ); //$NON-NLS-1$
									nuevaR = marcasAverias.addResistencia( pIni, pFin );
									nuevaR.initResistencias( marcasAverias.getCombsInterrupcion().size() );
								}
								setResistencias( nuevaR );
								marcasAverias.seleccionarElemento( nuevaR, true );
								refrescarTablaCon( nuevaR );
							}
							pIni = pFin;
						}
					}
				}
			}
		} else {  // Modo edición o voltaje (ni interruptor ni resistencias)
			hayCambiosEnCircuito = true;
			boolean edicionDePuntoDeElectronica = false;
			Object[] encimaDeConector = marcasAverias.enConexion( pInicioDragAbs );
			Componente compElectronica = null;
			if (odgDePanelEdicionActual!=null && odgDePanelEdicionActual instanceof Componente) {
				if (((Componente)odgDePanelEdicionActual).isElectronica()) {
					compElectronica = (Componente)odgDePanelEdicionActual;
					if (((Componente)odgDePanelEdicionActual).estaPulsadoElecAnyadir()) {
						edicionDePuntoDeElectronica = true;
					} else if (((Componente)odgDePanelEdicionActual).estaPulsadoElecQuitar()) {
						edicionDePuntoDeElectronica = true;
					}
				}
			}
			if (!v.bSeleccion.isSelected() || estado == Estado.VOLTAJES) { 
				marcasAverias.seleccionarElementos( false, null );  // Quitar selección si la hubiera
				actualizarSeleccionDeMarcasEnTabla();
			}
			quitaMarcas();
			if (!marcasAverias.existePunto(pDestinoDragAbs, v.pDibujo.getZoom()).equals(pDestinoDragAbs)) {  // Ya existe punto
				pDestinoDragAbs = marcasAverias.existePunto(pDestinoDragAbs, v.pDibujo.getZoom());
			} else if (e.isControlDown() && !marcasAverias.existePunto(pDestinoDragAbs,MarcasAverias.CERCANIA_PUNTOS_MAYOR, v.pDibujo.getZoom()).equals(pDestinoDragAbs)) {  // Ya existe punto con más amplitud 
				pDestinoDragAbs = marcasAverias.existePunto(pDestinoDragAbs,MarcasAverias.CERCANIA_PUNTOS_MAYOR, v.pDibujo.getZoom());
			} else {
				if (!v.bSeleccion.isSelected() && (v.cbAEjes.isSelected() || e.isShiftDown())) { // Forzar a ejes
					if (Math.abs(pInicioDragAbs.x-pDestinoDragAbs.x) > Math.abs(pInicioDragAbs.y-pDestinoDragAbs.y)) {  // Forzar a horizontal
						pDestinoDragAbs.y = pInicioDragAbs.y;
					} else {  // Forzar a vertical
						pDestinoDragAbs.x = pInicioDragAbs.x;
					}
				}
			}
			if (edicionDePuntoDeElectronica && compElectronica!=null) {  
				ObjetoDeGrafico[] odgsSel = null;
				// int numObjsAnteriores = marcasAverias.getNumElementosSeleccionados();
				marcasAverias.memorizaSeleccion();
				if (e.getPoint().equals( inicioDrag )) {  // CLICK (punto inicio = destino)
					marcasAverias.seleccionarElementos( false, pInicioDragAbs, Punto.class );
					odgsSel = marcasAverias.getElementosSeleccionados();
					marcasAverias.restauraSeleccion();
				} else {  // DRAG (punto inicio != destino)
					marcasAverias.seleccionarElementos( false, pInicioDragAbs, pDestinoDragAbs, false, Punto.class );
					odgsSel = marcasAverias.getElementosSeleccionados();
				}
				boolean modificadoComponente = false;
				for (ObjetoDeGrafico odg : odgsSel) {
					if (odg instanceof Punto) {
						if (compElectronica.estaPulsadoElecAnyadir()) {  // Botón añadir
							if (!compElectronica.getAL().contains(odg)) {  // Si es un punto que no estaba ya, añadirlo
								compElectronica.getAL().add( (Punto)odg );
								modificadoComponente = true;
							}
						} else {  // Boton quitar
							if (compElectronica.getAL().contains(odg)) {  // Si es un punto que estaba ya, quitarlo
								compElectronica.getAL().remove( (Punto)odg );
								modificadoComponente = true;
							}
						}
					}
				}
				if (modificadoComponente) {
					if (compElectronica.getAL().isEmpty()) {  // Se ha quedado sin puntos: quitarlo
						marcasAverias.borraElemento( compElectronica );
						compElectronica = null;
					}
					refrescarTablaConBorrado();
					if (compElectronica != null) marcasAverias.seleccionarElemento( compElectronica, false );
				}
				marcasAverias.restauraSeleccion();
				redibujaCircuito();
			} else { // Cualquier otra cosa que no sean puntos de electrónica
				if (e.getPoint().equals( inicioDrag ) && !enPuntosAdicsComponente) {  // CLICK (punto inicio = destino)
					if (v.bSeleccion.isSelected() || estado==Estado.VOLTAJES) {  // Seleccionar elementos
						seleccionarObjetosPunto( e.isControlDown() );
					} else if (v.bPunto.isSelected()) {  // Añadir punto de conexión si no existe
						if (marcasAverias.isModoEdicion())
							if (!(pInicioDragAbs instanceof Punto)) { // Si no es punto es point y es que no se ha añadido antes
								Acciones.getInstance().hacer( "Añadir punto" ); //$NON-NLS-1$
								Punto puntoNuevo = marcasAverias.addPunto( pInicioDragAbs );
								Acciones.getInstance().limpiarSiNoAccion();
								refrescarTablaCon( puntoNuevo );
							}
					}
				} else {  // DRAG or select  (punto inicio != destino)
					if (v.bSeleccion.isSelected() || estado==Estado.VOLTAJES) {  
						seleccionarObjetosDrag( e.isControlDown() );
					} else if (marcasAverias.isModoEdicion()) {
						// Drag o puntos adicionales componente
						if (v.bConexion.isSelected()) {  // Añadir conexión
							Acciones.getInstance().hacer( "Añadir conexión" ); //$NON-NLS-1$
							Conexion nuevaC = marcasAverias.addConexion( true, pInicioDragAbs, pDestinoDragAbs );
							refrescarTablaCon( nuevaC );
						} else if (v.bConector.isSelected()) {  // Añadir conector
							Acciones.getInstance().hacer( "Añadir conector" ); //$NON-NLS-1$
							Conexion nuevoC = marcasAverias.addConector( pInicioDragAbs, pDestinoDragAbs );
							refrescarTablaCon( nuevoC );
						} else if (v.bComponente.isSelected()) {  // Añadir componente
							if (v.cbComponentes.getSelectedIndex()>=0) {
								String tipoComp = ((String)v.cbComponentes.getSelectedItem());
								if (tipoComp.equals("Elec")) {  // Componente especial: electrónica //$NON-NLS-1$
									ArrayList<Point> lPuntos = marcasAverias.buscaPuntosEnZona(pInicioDragAbs, pDestinoDragAbs);
									if (lPuntos.size()<2) {
										JOptionPane.showMessageDialog( null, Messages.getString("ControladorVentanaEAA.162") +  //$NON-NLS-1$
												Messages.getString("ControladorVentanaEAA.163"),  //$NON-NLS-1$
												Messages.getString("ControladorVentanaEAA.164"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$
									} else {
										Acciones.getInstance().hacer( "Añadir componente" ); //$NON-NLS-1$
										Point pInicio = lPuntos.remove(0);
										Componente nuevoC = marcasAverias.addComponente( true, tipoComp, pInicio, lPuntos );
										refrescarTablaCon( nuevoC );
									}
								} else {  // Cualquier otro componente
									int numPuntos;
									if (!enPuntosAdicsComponente) {  // Inicio de componente
										numPuntos = 2;
										inicioEdComponente(tipoComp);
									} else {  // Puntos adicionales
										if (pInicioCompon!=pDestinoDragAbs && !puntosAdics.contains(pDestinoDragAbs)) {
											puntosAdics.add( pDestinoDragAbs );
										} // else el último punto ya estaba, no se introduce, sigue igual
										numPuntos = 1 + puntosAdics.size();  
									}
									if (numPuntos==Componente.getNumPuntos( tipoComp )) {
										Acciones.getInstance().hacer( "Añadir componente" ); //$NON-NLS-1$
										Componente nuevoC = marcasAverias.addComponente( true, tipoComp, pInicioCompon, puntosAdics );
										refrescarTablaCon( nuevoC );
										finEdComponente();
									} else {
										v.setMensaje( Messages.getString("ControladorVentanaEAA.167") + (numPuntos+1) + Messages.getString("ControladorVentanaEAA.168") + tipoComp ); //$NON-NLS-1$ //$NON-NLS-2$
									}
								}
							}
						} else if (v.bMover.isSelected()) {  // Mover elementos
							if (pInicioDragAbs instanceof Punto) {  // Si el inicio era un punto... moverlo al final
								if (pDestinoDragAbs instanceof Punto) {  // Si el destino es un punto... se fusionan
									moverPuntoSobreOtroPunto( (Punto) pInicioDragAbs, (Punto) pDestinoDragAbs );
								} else {
									// Mover un punto a otro sitio diferente
									Acciones.getInstance().hacer( "Mover punto" ); //$NON-NLS-1$
									marcasAverias.moverPunto( (Punto) pInicioDragAbs, pDestinoDragAbs );
									redibujaCircuito();
									refrescarTablaCon( (Punto)pInicioDragAbs );
								}
							} else if (marcasAverias.getTextoVisualDe(pInicioDragAbs)!=null) {  // Si el inicio es un texto... moverlo de sitio
								Acciones.getInstance().hacer( "Mover texto" ); //$NON-NLS-1$
								TextoDeCircuito tc = marcasAverias.getTextoVisualDe(pInicioDragAbs);
								marcasAverias.moverTexto( tc, pDestinoDragAbs.x-pInicioDragAbs.x, pDestinoDragAbs.y-pInicioDragAbs.y );
							}
						} else if (v.bPunto.isSelected()) {  // Modo punto - posible creación de un punto nuevo encima de un conector
							if (encimaDeConector!=null) {
								Acciones.getInstance().hacer( "Modificar conexión" ); //$NON-NLS-1$
								marcasAverias.addPuntoAConexion( ((Conexion)encimaDeConector[0]), pDestinoDragAbs, ((Integer) encimaDeConector[1]) );
								redibujaCircuito();
								refrescarTablaCon( ((Conexion)encimaDeConector[0]) );
							}
						}
					}
				}
			}
		}
		inicioDrag = null;
	}
	void mousePressed( MouseEvent e ) {
		inicioDrag = e.getPoint();  // Point
		pInicioDragAbs = new Point( v.pDibujo.getCoordLogica(inicioDrag.x), v.pDibujo.getCoordLogica(inicioDrag.y) );
		logger.log( Level.FINEST, "MousePressed " + e.getPoint() + " - Escalado " + pInicioDragAbs ); //$NON-NLS-1$ //$NON-NLS-2$
		pDestinoDragAbs = null;
		marcasAverias.setMarcaLinea( null ); marcasAverias.setMarcaRect( null ); marcasAverias.setMarcaPunto( inicioDrag );
		if (e.isControlDown()) { // Con control, amplía el rango de cercanía de punto
			if (marcasAverias.existePunto(pInicioDragAbs, MarcasAverias.CERCANIA_PUNTOS_MAYOR, v.pDibujo.getZoom()) instanceof Punto) { // Ya existe punto 
				pInicioDragAbs = marcasAverias.existePunto(pInicioDragAbs, MarcasAverias.CERCANIA_PUNTOS_MAYOR, v.pDibujo.getZoom());  // Devuelve punto si existe (si no sigue siendo Point)
			}
		} else {
			if (marcasAverias.existePunto(pInicioDragAbs, v.pDibujo.getZoom()) instanceof Punto) { // Ya existe punto
				pInicioDragAbs = marcasAverias.existePunto(pInicioDragAbs, v.pDibujo.getZoom());  // Devuelve punto si existe (si no sigue siendo Point)
			}
		}
		marcaInicioDrag();
	}

		// Gestión de ratón - métodos particulares de edición de componente
		private void inicioEdComponente(String tipoComp) {
			puntosAdics = new ArrayList<Point>();
			puntosAdics.add( pDestinoDragAbs );
			pInicioCompon = pInicioDragAbs;
			if (2<Componente.getNumPuntos( tipoComp )) {  // Si quedan más puntos por determinar
				enPuntosAdicsComponente = true;
				v.cbComponentes.setEnabled( false );
				v.bSeleccion.setEnabled( false ); v.bPunto.setEnabled( false ); v.bConexion.setEnabled( false ); v.bConector.setEnabled( false ); v.bMover.setEnabled( false ); v.bBorrar.setEnabled( false );
			}
		}
		private void finEdComponente() {
			enPuntosAdicsComponente = false;
			v.setMensaje( " " ); //$NON-NLS-1$
			v.cbComponentes.setEnabled( true );
			v.bSeleccion.setEnabled( true ); v.bPunto.setEnabled( true ); v.bConexion.setEnabled( true ); v.bConector.setEnabled( true ); v.bMover.setEnabled( true ); v.bBorrar.setEnabled( true );
		}
	
	
	
	//
	//
	// Gestión de puntos
	//
	//
	
	private void moverPuntoSobreOtroPunto( Punto ptoInicio, Punto ptoDestino ) {
		// Comprobar a qué estructuras pertenecen los puntos
		ArrayList<ObjetoDeGrafico> lObjsPto1 = marcasAverias.getObjetosDePunto( ptoInicio );
		ArrayList<ObjetoDeGrafico> lObjsPto2 = marcasAverias.getObjetosDePunto( ptoDestino );
		// Casos (precedencia siempre para el caso menor)
		// Caso 1: ambos puntos pertenecen al mismo componente: se intercambian su posición en el componente 
		// Caso 2: ambos puntos pertenecen a diferentes componentes: no se hace nada 
		// Caso 3: ambos puntos pertenecen al mismo conector: se quita ese conector (si son contiguos - si no nada)
		// Caso 4: ambos puntos pertenecen a la misma conexión: se quita esa conexión (si son contiguos - si no nada)
		// Caso 5: ambos puntos pertenecen a diferentes conectores: se unen los conectores (si están en extremos - si no nada)
		// Caso 6: ambos puntos pertenecen a diferentes conexiones: se unen las conexiones (si están en extremos - si no nada)
		// Caso 99: ninguno de los anteriores: se pierde uno de los puntos
		int caso = 99;
		ObjetoDeGrafico contenedor = null;
		ObjetoDeGrafico contenedor2 = null;
		marcaCierre:
			for (ObjetoDeGrafico odg1 : lObjsPto1) {
				for (ObjetoDeGrafico odg2 : lObjsPto2) {
					if (odg1.getTipoElemento()==TipoElemento.Componente && odg2.getTipoElemento()==TipoElemento.Componente) {
						if (odg1==odg2) {  // Caso 1
							caso = 1; contenedor = odg1; break marcaCierre;
						} else {  // Caso 2
							caso = 2; break marcaCierre;
						}
					} else if (odg1.getTipoElemento()==TipoElemento.Conector && odg2.getTipoElemento()==TipoElemento.Conector && odg1==odg2) {  // Caso 3
						caso = 3; contenedor = odg1;
					} else if (caso>4 && odg1.getTipoElemento()==TipoElemento.Conexion && odg2.getTipoElemento()==TipoElemento.Conexion && odg1==odg2) {  // Caso 4
						caso = 4; contenedor = odg1;
					} else if (caso>5 && odg1.getTipoElemento()==TipoElemento.Conector && odg2.getTipoElemento()==TipoElemento.Conector && odg1!=odg2) {  // Caso 5
						caso = 5; contenedor = odg1; contenedor2 = odg2;
					} else if (caso>6 && odg1.getTipoElemento()==TipoElemento.Conexion && odg2.getTipoElemento()==TipoElemento.Conexion && odg1!=odg2) {  // Caso 6
						caso = 6; contenedor = odg1; contenedor2 = odg2;
					}
				}
			}
		logger.log( Level.FINEST, "Mover punto sobre otro punto " + ptoInicio + " a " + ptoDestino + " (caso " + caso + ")" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		if (caso==1) {
			hayCambiosEnCircuito = true;
			Acciones.getInstance().hacer( "Cambiar punto por otro" ); //$NON-NLS-1$
			Componente comp = (Componente)contenedor;
			int pos1 = comp.getAL().indexOf( ptoInicio );
			int pos2 = comp.getAL().indexOf( ptoDestino );
			marcasAverias.setPuntoCo( comp, pos1, ptoDestino ); marcasAverias.setPuntoCo( comp, pos2, ptoInicio );  // Intercambio
		} else if (caso==2) {
			return;
		} else if (caso==3 || caso==4) {
			hayCambiosEnCircuito = true;
			Conexion con = (Conexion)contenedor;
			int pos1 = con.getAL().indexOf( ptoInicio );
			int pos2 = con.getAL().indexOf( ptoDestino );
			if (Math.abs(pos1-pos2)==1) {
				Acciones.getInstance().hacer( "Cambiar punto por otro" ); //$NON-NLS-1$
				if (con.getAL().size()==2) {  // Si la conexión-conector solo tiene 2, se quita
					marcasAverias.borraElemento( con );
					marcasAverias.cambiaPunto( ptoInicio, ptoDestino );
				} else {  // Si tiene más, se mantiene con un punto menos
					marcasAverias.removePuntoCo( con, pos1 );
					marcasAverias.cambiaPunto( ptoInicio, ptoDestino );
				}
			} else return;
		} else if (caso==5 || caso==6) {
			hayCambiosEnCircuito = true;
			Acciones.getInstance().hacer( "Unir conexiones/conectores" ); //$NON-NLS-1$
			Conexion con = (Conexion)contenedor2;  //Destino drag - conexión que permanece
			Conexion con2 = (Conexion)contenedor;  //Origen drag - conexión que se pierde
			int pos1 = con.getAL().indexOf( ptoDestino );
			int pos2 = con2.getAL().indexOf( ptoInicio );
			if (pos1>0 && pos1<con.getAL().size()-1) return;  // No en extremo: nada
			if (pos2>0 && pos2<con2.getAL().size()-1) return; // No en extremo: nada
			if (pos1==0 && pos2==0) for (int pos=1; pos<con2.getAL().size(); pos++) marcasAverias.addPuntoCo( con, 0, con2.getAL().get(pos) );  // Añadir por el final en mismo orden
			else if (pos1>0 && pos2>=0) for (int pos=con2.getAL().size()-2; pos>=0; pos--) marcasAverias.addPuntoCo( con, con2.getAL().get(pos) );  // Añadir por el inicio en mismo orden
			else if (pos1>0 && pos2==0) for (int pos=1; pos<con2.getAL().size(); pos++) marcasAverias.addPuntoCo( con, con2.getAL().get(pos) );  // Añadir por el inicio en orden inverso
			else for (int pos=con2.getAL().size()-2; pos>=0; pos--) marcasAverias.addPuntoCo( con, 0, con2.getAL().get(pos) );  // Añadir por el final en orden inverso
			marcasAverias.borraElemento( con2 );
			marcasAverias.borraElemento( ptoInicio );
		} else {
			// Caso general: decidir cuál se pierde
			hayCambiosEnCircuito = true;
			Acciones.getInstance().hacer( "Cambiar punto por otro" ); //$NON-NLS-1$
			Punto ptoSePierde = ptoInicio; Punto ptoSeQueda = ptoDestino;
			if (ptoDestino.nombrePuntoEstandar()) {
				ptoSePierde = ptoDestino; ptoSeQueda = ptoInicio;
			}
			// Cambiar un punto por otro
			marcasAverias.cambiaPunto( ptoSePierde, ptoSeQueda );
		}
		cargarTabla(); // Refrescar por si acaso la tabla siempre
	}


	//
	//
	// Marcas de selección y arrastre en pantalla
	//
	//
	
	private void marcaInicioDrag() {
		marcasAverias.setMarcaPunto( pInicioDragAbs );
		redibujaCircuito();
	}

	private void marcaLineaDrag( Point p ) {
		marcasAverias.setMarcaLinea( p );
		redibujaCircuito();
	}

	private void marcaRectDrag( Point p ) {
		marcasAverias.setMarcaRect( p );
		redibujaCircuito();
	}

	private void quitaMarcas() {
		marcasAverias.setMarcaPunto( null );
		marcasAverias.setMarcaLinea( null );
		marcasAverias.setMarcaRect( null );
		redibujaCircuito();
	}


	//
	//
	// Métodos de selección
	//
	//

	// Seleccionar elementos con click
	private void seleccionarObjetosPunto( boolean anyadirASelExistente ) {
		if (marcasAverias.isModoEdicion())
			marcasAverias.seleccionarElementos( anyadirASelExistente, pInicioDragAbs );
		else  
			if (estado==Estado.RESISTENCIAS) // En modo de resistencias solo se seleccionan las resistencias
				marcasAverias.seleccionarElementos( anyadirASelExistente, pInicioDragAbs, Resistencia.class );
			else // En modo de edición de voltajes solo se seleccionan los puntos 
				marcasAverias.seleccionarElementos( anyadirASelExistente, pInicioDragAbs, Punto.class );
		actualizarSeleccionDeMarcasEnTabla();
	}

	// Seleccionar elementos con drag (da igual el modo)
	private void seleccionarObjetosDrag( boolean anyadirASelExistente ) {
		boolean tocando = escTecladoTextos.isShiftPulsado();
		if (marcasAverias.isModoEdicion())
			marcasAverias.seleccionarElementos( anyadirASelExistente, pInicioDragAbs, pDestinoDragAbs, tocando );
		else  
			if (estado==Estado.RESISTENCIAS) // En modo resistencias solo se seleccionan las resistencias
				marcasAverias.seleccionarElementos( anyadirASelExistente, pInicioDragAbs, pDestinoDragAbs, tocando, Resistencia.class );
			else // En modo de edición de voltajes solo se seleccionan los puntos 
				marcasAverias.seleccionarElementos( anyadirASelExistente, pInicioDragAbs, pDestinoDragAbs, false, Punto.class );
		actualizarSeleccionDeMarcasEnTabla();
	}

	private void borrarSeleccion() {
		if (marcasAverias.getNumElementosSeleccionados()>0) {
			int conf = JOptionPane.showConfirmDialog( v, 
					Messages.getString("ControladorVentanaEAA.1") + marcasAverias.getNumElementosSeleccionados() + Messages.getString("ControladorVentanaEAA.183"),  //$NON-NLS-1$ //$NON-NLS-2$
					Messages.getString("ControladorVentanaEAA.184"), JOptionPane.YES_NO_OPTION ); //$NON-NLS-1$
			if (conf==0) {
				Acciones.getInstance().hacer( Messages.getString("ControladorVentanaEAA.185") ); //$NON-NLS-1$
				marcasAverias.borraElementosSeleccionados();
				refrescarTablaConBorrado();
			}
		}
	}


	//
	//
	// Métodos de gestión de la JTable derecha de componentes
	//
	//
	
	// Modelo personalizado de tabla
	protected void initTabla() {
		odgsEnTablaDerecha = new ArrayList<ObjetoDeGrafico>();
		v.tablaDerecha = new JTable();
		modeloTablaDerecha = new DefaultTableModel( new Object[] { Messages.getString("ControladorVentanaEAA.186"), Messages.getString("ControladorVentanaEAA.187") }, 0 ) { //$NON-NLS-1$ //$NON-NLS-2$
			private static final long serialVersionUID = 1L;
			@Override
			public void setValueAt(Object aValue, int row, int column) {
				super.setValueAt(aValue, row, column);
				ObjetoDeGrafico odgEditado = odgsEnTablaDerecha.get( row );
				if (column==0 && !odgEditado.getNombre().equals(aValue)) {
					System.out.println( "CAMBIO!!!" ); //$NON-NLS-1$
					odgEditado.setVal( "Nombre", aValue ); //$NON-NLS-1$
				}
			}
		};
		v.tablaDerecha.setModel( modeloTablaDerecha );
	}

	/** Carga la tabla derecha de nuevo completa
	 */
	@SuppressWarnings("unchecked")
	public void cargarTabla() {
		int antSeleccion = v.tablaDerecha.getSelectedRow();
		// Vaciar modelo
		for (int r =  modeloTablaDerecha.getRowCount()-1; r >= 0; r--) modeloTablaDerecha.removeRow( r );
		odgsEnTablaDerecha.clear();
		// tablaDerecha.setModel( modeloTablaDerecha );
		// Poner nombre cabecera según el estado
		if (estadoOrdenNombre==0) {  // Sin orden
			v.tablaDerecha.getTableHeader().getColumnModel().getColumn(0).setHeaderValue( "Nombre" );
		} else if (estadoOrdenNombre==1) {  // Orden de nombre
			v.tablaDerecha.getTableHeader().getColumnModel().getColumn(0).setHeaderValue( "Nombre ^" );
		} else {  // Orden inverso
			v.tablaDerecha.getTableHeader().getColumnModel().getColumn(0).setHeaderValue( "Nombre v" );
		}
		// Llenar modelo con lo que corresponda (considerando el estado de orden)
		if (v.cbTiposElementos.getSelectedItem().equals("Conexion") || v.cbTiposElementos.getSelectedItem().equals("Todos")) {
			ArrayList<Conexion> listaC = (ArrayList<Conexion>) marcasAverias.getConexiones().clone();
			if (estadoOrdenNombre==1) listaC.sort( new Comparator<Conexion>() { @Override public int compare(Conexion o1, Conexion o2) {
				return o1.getNombre().compareTo(o2.getNombre());
			} }); else if (estadoOrdenNombre==2) listaC.sort( new Comparator<Conexion>() { @Override public int compare(Conexion o1, Conexion o2) {
				return o2.getNombre().compareTo(o1.getNombre());
			} });
			for (Conexion c : listaC) {
				modeloTablaDerecha.addRow( new Object[] { c.getNombre(), c.toDescripcion() } );
				odgsEnTablaDerecha.add( c );
			}
		}
		if (v.cbTiposElementos.getSelectedItem().equals("Conector") || v.cbTiposElementos.getSelectedItem().equals("Todos")) {
			ArrayList<Conector> listaC = (ArrayList<Conector>) marcasAverias.getConectores().clone();
			if (estadoOrdenNombre==1) listaC.sort( new Comparator<Conector>() { @Override public int compare(Conector o1, Conector o2) {
				return o1.getNombre().compareTo(o2.getNombre());
			} }); else if (estadoOrdenNombre==2) listaC.sort( new Comparator<Conector>() { @Override public int compare(Conector o1, Conector o2) {
				return o2.getNombre().compareTo(o1.getNombre());
			} });
			for (Conector c : listaC) {
				modeloTablaDerecha.addRow( new Object[] { c.getNombre(), c.toDescripcion() } );
				odgsEnTablaDerecha.add( c );
			}
		}
		if (v.cbTiposElementos.getSelectedItem().equals("Punto") || v.cbTiposElementos.getSelectedItem().equals("Todos")) {
			ArrayList<Punto> lista = (ArrayList<Punto>) marcasAverias.getPuntos().clone();
			if (estadoOrdenNombre==1) lista.sort( new Comparator<Punto>() { @Override public int compare(Punto o1, Punto o2) {
				return o1.getNombre().compareTo(o2.getNombre());
			} }); else if (estadoOrdenNombre==2) lista.sort( new Comparator<Punto>() { @Override public int compare(Punto o1, Punto o2) {
				return o2.getNombre().compareTo(o1.getNombre());
			} });
			for (Punto p : lista) {
				modeloTablaDerecha.addRow( new Object[] { p.getNombre(), p.toDescripcion() } );
				odgsEnTablaDerecha.add( p );
			}
		} 
		if (v.cbTiposElementos.getSelectedItem().equals("Componente") || v.cbTiposElementos.getSelectedItem().equals("Todos")) {
			ArrayList<Componente> lista = (ArrayList<Componente>) marcasAverias.getComponentes().clone();
			if (estadoOrdenNombre==1) lista.sort( new Comparator<Componente>() { @Override public int compare(Componente o1, Componente o2) {
				return o1.getNombre().compareTo(o2.getNombre());
			} }); else if (estadoOrdenNombre==2) lista.sort( new Comparator<Componente>() { @Override public int compare(Componente o1, Componente o2) {
				return o2.getNombre().compareTo(o1.getNombre());
			} });
			for (Componente c : lista) {
				modeloTablaDerecha.addRow( new Object[] { c.getNombre(), c.toDescripcion() } );
				odgsEnTablaDerecha.add( c );
			}
		}
		if (v.cbTiposElementos.getSelectedItem().equals("Resistencia") || v.cbTiposElementos.getSelectedItem().equals("Todos")) {
			ArrayList<Resistencia> lista = (ArrayList<Resistencia>) marcasAverias.getResistencias().clone();
			if (estadoOrdenNombre==1) lista.sort( new Comparator<Resistencia>() { @Override public int compare(Resistencia o1, Resistencia o2) {
				return o1.getNombre().compareTo(o2.getNombre());
			} }); else if (estadoOrdenNombre==2) lista.sort( new Comparator<Resistencia>() { @Override public int compare(Resistencia o1, Resistencia o2) {
				return o2.getNombre().compareTo(o1.getNombre());
			} });
			for (Resistencia r : lista) {
				modeloTablaDerecha.addRow( new Object[] { r.getNombre(), r.toDescripcion() } );
				odgsEnTablaDerecha.add( r );
			}
		}
		v.tablaDerecha.getSelectionModel().setSelectionInterval(antSeleccion,antSeleccion);
	}

	/** Refresca la tabla de la derecha con cambio en el odg indicado
	 * @param odg	Objeto que hay que actualizar en la tabla
	 */
	public void refrescarTablaCon( ObjetoDeGrafico odg ) {
		if (v.cbTiposElementos.getSelectedItem().equals(Messages.getString("ControladorVentanaEAA.200")) || //$NON-NLS-1$   Todos
				(odg!=null && v.cbTiposElementos.getSelectedItem().equals( odg.getTipoElemento().toString() )))
			cargarTabla();
	}

	// Refresca la tabla si hay borrados
	protected void refrescarTablaConBorrado() {
		cargarTabla();
	}

	protected void actualizarSeleccionDeMarcasEnTabla() {
		// Chequear si hay que cambiar la selección en la tabla (si hay un solo tipo de elementos ponerlo, si hay varios poner "todos")
		HashSet<Class<?>> tiposSeleccionados = new HashSet<>();
		for (ObjetoDeGrafico odg : marcasAverias.getConjElementosSeleccionados()) {
			tiposSeleccionados.add( odg.getClass() );
		}
		flagCambiandoSeleccionTabla = true;  // Marca para que no actualice la selección al borrar la tabla
		if (tiposSeleccionados.size()==1) {  // Solo un tipo seleccionado - cambiar la selección de la tabla si procede
			if (!v.cbTiposElementos.getSelectedItem().equals( marcasAverias.getElementosSeleccionados()[0].getTipoElemento()) &&
				!v.cbTiposElementos.getSelectedItem().equals( "Todos" ) ) {
				v.cbTiposElementos.setSelectedItem( marcasAverias.getElementosSeleccionados()[0].getTipoElemento().toString() );
				cargarTabla();
			}
		} else if (tiposSeleccionados.size()>1) {  // Varios tipos seleccionados - cambiar la selección de la tabla a "todos"
			if (!v.cbTiposElementos.getSelectedItem().equals( "Todos" ) ) {
				v.cbTiposElementos.setSelectedItem( "Todos" );
				cargarTabla();
			}
		}
		// Actualizar la tabla con los elementos seleccionados
		v.tablaDerecha.getSelectionModel().clearSelection();
		int numElsSels = 0; int numNoPuntos = 0; int numResistencias = 0;
		ObjetoDeGrafico objSel = null; ObjetoDeGrafico objNoPuntoSel = null; ObjetoDeGrafico objNoPuntoNoResistenciaSel = null;
		for (ObjetoDeGrafico odg : marcasAverias.getElementosSeleccionados()) {
			int posi = odgsEnTablaDerecha.indexOf( odg );
			if (posi>-1) {
				v.tablaDerecha.getSelectionModel().addSelectionInterval(posi,posi);
				objSel = odg;
				numElsSels++;
				if (!(objSel instanceof Punto)) {
					numNoPuntos++; objNoPuntoSel = objSel; 
					if (objSel instanceof Resistencia) numResistencias++;
					else objNoPuntoNoResistenciaSel = objSel;
				}
			}
		}
		flagCambiandoSeleccionTabla = false;
		calcSeleccionYPanel(numElsSels, numNoPuntos, numResistencias, objSel, objNoPuntoSel, objNoPuntoNoResistenciaSel);
	}

	protected void actualizarSeleccionDeTablaEnMarcas() {
		marcasAverias.seleccionarElementos(false, null); // Vacia la selección
		int numElsSels = 0; int numNoPuntos = 0; int numResistencias = 0;
		ObjetoDeGrafico objSel = null; ObjetoDeGrafico objNoPuntoSel = null; ObjetoDeGrafico objNoPuntoNoResistenciaSel = null;
		for (int indice = 0; indice < odgsEnTablaDerecha.size(); indice++) {
			if (v.tablaDerecha.getSelectionModel().isSelectedIndex(indice)) {
				marcasAverias.anyadirSeleccionElemento( odgsEnTablaDerecha.get(indice) );
				objSel = odgsEnTablaDerecha.get(indice);
				numElsSels++;
				if (!(objSel instanceof Punto)) {
					numNoPuntos++; objNoPuntoSel = objSel; 
					if (objSel instanceof Resistencia) numResistencias++;
					else objNoPuntoNoResistenciaSel = objSel;
				}
			}
		}
		calcSeleccionYPanel(numElsSels, numNoPuntos, numResistencias, objSel, objNoPuntoSel, objNoPuntoNoResistenciaSel);
		redibujaCircuito();
	}
		private void calcSeleccionYPanel(int numElsSels, int numNoPuntos, int numResistencias, ObjetoDeGrafico objSel, ObjetoDeGrafico objNoPuntoSel, ObjetoDeGrafico objNoPuntoNoResistenciaSel) {
			v.pAtributos.removeAll();
			odgDePanelEdicionActual = null;
			if (numElsSels == 1 || numNoPuntos == 1) {  // Si hay un elemento seleccionado sacar el panel de edición
				if (numElsSels == 1) odgDePanelEdicionActual = objSel; else odgDePanelEdicionActual = objNoPuntoSel;
				v.pAtributos.add( odgDePanelEdicionActual.getPanelEdicion(), BorderLayout.CENTER );
			} else if (numNoPuntos - numResistencias == 1) {  // Si sólo hay un elemento principal seleccionado que no sea resistencia
				odgDePanelEdicionActual = objNoPuntoNoResistenciaSel;
				v.pAtributos.add( odgDePanelEdicionActual.getPanelEdicion(), BorderLayout.CENTER );
			} else {  // Si hay varios elementos seleccionados
				JPanel panelEdicionDeVarios = ControladorVentanaEAA.getPanelEdicionGrupo( marcasAverias.getElementosSeleccionados() );
				if (panelEdicionDeVarios!=null) {
					v.pAtributos.add( panelEdicionDeVarios, BorderLayout.CENTER );
				} else  // Si no hay ninguno o hay varios no compatibles quitar el panel de edición (panel vacío)
					v.pAtributos.add( new JPanel(), BorderLayout.CENTER ); 
			}
			v.pDerecho.revalidate();
			v.pDerecho.repaint();
			scrollToVisible( v.tablaDerecha );
		}
		private void scrollToVisible(final JTable table) {
			SwingUtilities.invokeLater( new Runnable() {
				@Override
				public void run() {
					if (table.getSelectedRow()!=-1) {
				        Rectangle rect = table.getCellRect( table.getSelectedRow(), 0, true);
				        //JViewport viewport = (JViewport)table.getParent();
				        // Point pt = viewport.getViewPosition();
						// System.out.println( table.getSelectedRow() + " -> " + rect + " | " + pt);
				        // rect.setLocation(rect.x-pt.x, rect.y-pt.y);
				        table.scrollRectToVisible(rect);
					}
				}
			});
	    }
		
		private static JPanel miPanelEdicionGrupo = null;
		private static ArrayList<ObjetoDeGrafico> miListaObjetosEnPanel = null;
		public static JPanel getPanelEdicionGrupo( ObjetoDeGrafico[] odgs ) {
			ArrayList<ObjetoDeGrafico> compsSeleccionados = new ArrayList<>();
			TipoElemento elTipo = null;
			for (ObjetoDeGrafico odg : odgs) {
				// System.out.println( "Seleccionado: " + odg );
				compsSeleccionados.add( odg );
				if (elTipo==null) 
					elTipo = odg.getTipoElemento();
				else if (elTipo!=odg.getTipoElemento()) {
					elTipo = null;
					break;
				} // else {  // tipos iguales
			}
			// Aquí llegan calculados compSeleccionados y elTipo (si es común)
			if (miPanelEdicionGrupo != null) {
				if (miListaObjetosEnPanel.equals(compsSeleccionados))
					return miPanelEdicionGrupo;
				miPanelEdicionGrupo = null;
			}
			if (elTipo==null) return null;
			if (compsSeleccionados.size() == 0) return null;
			miListaObjetosEnPanel = compsSeleccionados;
			miPanelEdicionGrupo = new JPanel();
			miPanelEdicionGrupo.setMinimumSize( new Dimension( 100, 150 ));
			miPanelEdicionGrupo.setLayout( new BoxLayout( miPanelEdicionGrupo, BoxLayout.Y_AXIS ));
			if (elTipo == TipoElemento.Componente) {
				JPanel linea1 = new JPanel();
				miPanelEdicionGrupo.add( linea1 );
				JCheckBox cbSeleccionable = new JCheckBox( Messages.getString("ControladorVentanaEAA.201") ); //$NON-NLS-1$
					linea1.add( cbSeleccionable );
				// Eventos
				cbSeleccionable.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						hayCambiosEnCircuito = true;
						for (ObjetoDeGrafico odg : miListaObjetosEnPanel) {
							Componente comp = (Componente) odg;
							comp.setSeleccionable( ((JCheckBox)(e.getSource())).isSelected() );
						}
					}
				});
			} else if (elTipo == TipoElemento.Punto) {
				JPanel linea1 = new JPanel();
				miPanelEdicionGrupo.add( linea1 );
				JPanel linea2 = new JPanel();
				miPanelEdicionGrupo.add( linea2 );
				JCheckBox cbConectable = new JCheckBox( Messages.getString("ControladorVentanaEAA.202")); //$NON-NLS-1$
					linea1.add( cbConectable );
				JLabel lVoltaje = new JLabel( Messages.getString("ControladorVentanaEAA.203") ); //$NON-NLS-1$
					linea2.add( lVoltaje );
					JTextField tfVoltaje = new JTextField( 5 );
					tfVoltaje.setVisible( false );
					lVoltaje.setVisible( false );
				// Eventos
				cbConectable.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						hayCambiosEnCircuito = true;
						for (ObjetoDeGrafico odg : miListaObjetosEnPanel) {
							Punto p = (Punto) odg;
							p.setConectable( ((JCheckBox)(e.getSource())).isSelected() );
						}
					}
				});
				tfVoltaje.addFocusListener( new FocusAdapter() {
					String valAnterior = null;
					@Override public void focusGained(FocusEvent e) {
						valAnterior = ((JTextField)(e.getSource())).getText();
					}
					@Override public void focusLost(FocusEvent e) {
						if (!((JTextField)(e.getSource())).getText().equals(valAnterior)) {
							hayCambiosEnCircuito = true;
							String valVolt = ((JTextField)(e.getSource())).getText();
							for (ObjetoDeGrafico odg : miListaObjetosEnPanel) {
								Punto p = (Punto) odg;
								p.actualizaVoltajeDesdePanelEdicion( valVolt );
							}
						}
					}
				});
			} else if (elTipo == TipoElemento.Resistencia) {
				JPanel linea1 = new JPanel();
				miPanelEdicionGrupo.add( linea1 );
				linea1.add( new JLabel( Messages.getString("ControladorVentanaEAA.204") ) ); //$NON-NLS-1$
					JTextField tfResistencia = new JTextField( 5 );
					linea1.add( tfResistencia );
				// Eventos
				tfResistencia.addFocusListener( new FocusAdapter() {
					private String valAnterior = null;
					@Override public void focusGained(FocusEvent e) {
						valAnterior = ((JTextField)(e.getSource())).getText();
					}
					@Override public void focusLost(FocusEvent e) {
						if (!((JTextField)(e.getSource())).getText().equals(valAnterior)) {
							hayCambiosEnCircuito = true;
							if (VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().isModoResistencias()) {
								String texRes = ((JTextField)(e.getSource())).getText().toUpperCase();
								for (ObjetoDeGrafico odg : miListaObjetosEnPanel) {
									Resistencia res = (Resistencia)odg;
									if (VentanaEditorAveriaAuto.getLastVentana().isCombinacionTodasSeleccionada()) {
										try {
											double d = 0;
											if (texRes.equals(Messages.getString("ControladorVentanaEAA.3")) || texRes.equals(Messages.getString("ControladorVentanaEAA.2"))) d = Resistencia.RESISTENCIA_INFINITA; //$NON-NLS-1$ //$NON-NLS-2$
											else if (texRes.equals(Messages.getString("ControladorVentanaEAA.4"))) d = Resistencia.RESISTENCIA_INDEFINIDA; //$NON-NLS-1$
											else d = java.lang.Double.parseDouble( texRes );
											for (int i=0; i<res.getResistencias().size(); i++) {
												res.setResistencia( i, d );
											}
											VentanaEditorAveriaAuto.getLastVentana().getControlador().refrescarTablaCon( res );
										} catch (NumberFormatException ex) {
											VentanaEditorAveriaAuto.getLastVentana().setMensaje( Messages.getString("ControladorVentanaEAA.208") + texRes ); //$NON-NLS-1$
										}
									} else {
										int indi = VentanaEditorAveriaAuto.getLastVentana().getIndiceCombinacionSeleccionada();
										if (indi!=-1 && indi<res.getResistencias().size()) {
											try {
												double d = 0;
												if (texRes.equals(Messages.getString("ControladorVentanaEAA.209"))) d = Resistencia.RESISTENCIA_INFINITA; //$NON-NLS-1$
												else if (texRes.equals(Messages.getString("ControladorVentanaEAA.210"))) d = Resistencia.RESISTENCIA_INDEFINIDA; //$NON-NLS-1$
												else d = java.lang.Double.parseDouble( texRes );
												res.setResistencia( indi, d );
												VentanaEditorAveriaAuto.getLastVentana().getControlador().refrescarTablaCon( res );
											} catch (NumberFormatException ex) {
												VentanaEditorAveriaAuto.getLastVentana().setMensaje( Messages.getString("ControladorVentanaEAA.211") + texRes ); //$NON-NLS-1$
											}
										}
									}
									
								}
							}
						}
					}
				});
			} 
			return miPanelEdicionGrupo;
		}

	
	/** Método de callback para refrescar un objeto por si está en la tabla de la derecha
	 * @param odg
	 */
	public void refrescaObjetoEnTabla( ObjetoDeGrafico odg ) {
		int fila = 0;
		for (ObjetoDeGrafico odgEnTabla : odgsEnTablaDerecha) {
			if (odgEnTabla == odg) {
				modeloTablaDerecha.setValueAt( odg.getVal("nombre"), fila, 0 ); //$NON-NLS-1$
				modeloTablaDerecha.setValueAt( odg.toDescripcion(), fila, 1 );
			}
			fila++;
		}
	}
	
	protected void cambioSeleccionEnTabla( ListSelectionEvent e ) {
		if (!e.getValueIsAdjusting() && !flagCambiandoSeleccionTabla) {
			actualizarSeleccionDeTablaEnMarcas();
		}
	}
	
		private int estadoOrdenNombre = 0;  // 0 = sin orden, 1 = orden alfabético, 2 = orden inverso
	public void clickHeaderColumnaNombre() {
		estadoOrdenNombre++;
		if (estadoOrdenNombre==3) estadoOrdenNombre=0;
		cargarTabla();
	}

	
	//
	//
	// Modos de edición
	//
	//
	
	// Mira si todo ok para pasar a edición de averías
	private boolean listoParaAverias() {
		boolean listo = true;
		for (Componente c : marcasAverias.getComponentes()) {
			if (c.isInterruptor()) {
				if (c.getListaEstadosInt()==null) {  // Si algún interruptor no tiene estados definidos, no está listo
					listo = false; break;
				}
			}
		}
		return listo;
	}
	
	/** Activa el modo de edición en el interfaz de la ventana
	 * @param modoEdicion	true -> edición de circuito. false -> edición de voltajes, resistencias y averías
	 */
	public void setModoEdicion( boolean modoEdicion ) {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.213") + modoEdicion ); //$NON-NLS-1$
		AbstractButton[] botsEdicionCircuito = new AbstractButton[] { v.bComponente, v.bConector, v.bConexion, v.bMover, v.bPunto, v.bCalcCortes, v.bCargarGraf, v.bBorrar, v.bEditarAverias, v.bSeleccion };
		Component[] botsEdicionAverias = new Component[] { v.bV0, v.bVVariable, v.bV12, v.bVPNC, v.bVInterm, v.tfVVariable, v.bRecalcInterrupcion, v.bQuitaConectables, v.bPreCalculo, v.bEdiResistencias, v.bSeleccion };
		Component[] botsEdicionResistencias = new Component[] { v.bR0, v.bRInf, v.bRVariable, v.tfRVariable, v.bSeleccion };
		if (modoEdicion) {
			cambiaVisualModo( "Edi" ); // Pasa a modo visual edición //$NON-NLS-1$
			setEstado( Estado.CIRCUITO );
			for (Component c : botsEdicionAverias ) c.setVisible( false );
			for (Component c : botsEdicionResistencias ) c.setVisible( false );
			for (AbstractButton ab : botsEdicionCircuito ) ab.setVisible( true );
			v.cbAEjes.setVisible( true );
			v.cbComponentes.setVisible( true );
			v.spCombinacionesInterrupcion.setVisible( false );
		} else {
			hayCambiosEnCircuito = true;
			cambiaVisualModo( "Vol" ); // Pasa a modo visual voltaje //$NON-NLS-1$
			setEstado( Estado.VOLTAJES );
			for (AbstractButton ab : botsEdicionCircuito ) ab.setVisible( false );
			for (Component c : botsEdicionResistencias ) c.setVisible( false );
			for (Component c : botsEdicionAverias ) c.setVisible( true );
			v.bSeleccion.setSelected( true );  // Siempre seleccionado en modo voltaje
			v.cbAEjes.setVisible( false );
			v.cbComponentes.setVisible( false );
			v.spCombinacionesInterrupcion.setVisible( true );
			DefaultListModel<CombinacionInterrupcion> dlm = new DefaultListModel<CombinacionInterrupcion>();
			for (CombinacionInterrupcion ci : marcasAverias.getCombsInterrupcion()) dlm.addElement( ci );
			if (marcasAverias.getCombsInterrupcion().size()>1)
				dlm.addElement( new CombinacionInterrupcion() );  // Ultima combinación = todas  (solo si hay varias: si solo hay una no aparece)
			v.lCombinacionesInterrupcion.setModel( dlm );
		}
		marcasAverias.setModoEdicion( modoEdicion );
	}

	/** Activa el modo de resistencias en el interfaz de la ventana. Solo con modoEdicion=false
	 * @param modoEdicion	true -> edición de circuito. false -> edición de voltajes, resistencias y averías
	 */
	public void setModoResistencias( boolean modoResistencias ) {
		logger.log( Level.FINEST, Messages.getString("ControladorVentanaEAA.216") + modoResistencias ); //$NON-NLS-1$
		if (getEstado()==Estado.CIRCUITO) return;  // Con modo edición a true, no se activa el modo de resistencias
		Component[] botsEdicionAverias = new Component[] { v.bV0, v.bVVariable, v.bV12, v.bVPNC, v.bVInterm, v.tfVVariable, v.bRecalcInterrupcion, v.bQuitaConectables, v.bPreCalculo, v.bSeleccion };
		Component[] botsEdicionResistencias = new Component[] { v.bR0, v.bRVariable, v.bRInf,  v.tfRVariable, v.bSeleccion };
		if (modoResistencias) {
			cambiaVisualModo( "Res" ); // Pasa a modo visual resistencia //$NON-NLS-1$
			v.cbTiposElementos.setSelectedItem( "Resistencia" );
			v.bEdiResistencias.setText( Messages.getString("ControladorVentanaEAA.219") ); //$NON-NLS-1$
			setEstado( Estado.RESISTENCIAS );
			for (Component c : botsEdicionAverias ) c.setVisible( false );
			for (Component c : botsEdicionResistencias ) c.setVisible( true );
			v.bgBotonesSeleccion.remove( v.bSeleccion );
		} else {  // Modo voltaje
			cambiaVisualModo( "Vol" ); // Pasa a modo visual voltaje //$NON-NLS-1$
			v.cbTiposElementos.setSelectedItem( "Todos" );
			v.bEdiResistencias.setText( Messages.getString("ControladorVentanaEAA.222") ); //$NON-NLS-1$
			setEstado( Estado.VOLTAJES );
			for (Component c : botsEdicionResistencias ) c.setVisible( false );
			for (Component c : botsEdicionAverias ) c.setVisible( true );
			v.bgBotonesSeleccion.add( v.bSeleccion );
			v.bSeleccion.setSelected( true ); // Selección siempre activada en modo voltaje
		}
		marcasAverias.setModoResistencias( modoResistencias );
	}
	
		private void cambiaVisualModo( String modo ) {
			if (modo.equals("Res")) { //$NON-NLS-1$
				cambiaFondoPaneles( v.getContentPane(), Color.GREEN );
			} else if (modo.equals("Vol")) { //$NON-NLS-1$
				cambiaFondoPaneles( v.getContentPane(), Color.ORANGE );
			} if (modo.equals("Edi")) { //$NON-NLS-1$
				cambiaFondoPaneles( v.getContentPane(), Color.LIGHT_GRAY );
			}
		}
			private void cambiaFondoPaneles( Container cont, Color c ) {
				cont.setBackground( c );
				for (Component comp : cont.getComponents()) {
					if (comp instanceof Container) cambiaFondoPaneles( (Container)comp, c );
				}
			}

	
	//
	//
	// Voltajes
	//
	//
	
	// Método para poner voltajes en todos los puntos seleccionados
	
	public void setVoltajesPuntosSel( double volt ) {
		hayCambiosEnCircuito = true;
		int estadoSel = v.lCombinacionesInterrupcion.getSelectedIndex();
		if (estadoSel==-1) return;
		for (ObjetoDeGrafico odg : marcasAverias.getElementosSeleccionados()) {
			if (odg instanceof Punto) {
				Punto p = (Punto) odg;
				if (estadoSel == marcasAverias.getCombsInterrupcion().size()) {  // Última combinación = Todas
					for (int i=0; i<estadoSel; i++) 
						p.setVoltaje( i, volt );
				} else {
					p.setVoltaje( estadoSel, volt );
				}
			}
		}
		if (odgDePanelEdicionActual!=null) odgDePanelEdicionActual.getPanelEdicion();  // Refresca el panel con el voltaje (si procede)
	}

	
	//
	//
	// Resistencias
	//
	//
	
	// Método para poner resistencias en todas las resistencias indicadas
	private void setResistencias( double valorRes, Resistencia... ress ) {
		hayCambiosEnCircuito = true;
		int estadoSel = v.lCombinacionesInterrupcion.getSelectedIndex();
		if (estadoSel==-1) return;
		for (Resistencia r : ress) {
			if (estadoSel == marcasAverias.getCombsInterrupcion().size()) {  // Última combinación = Todas
				for (int i=0; i<estadoSel; i++) 
					r.setResistencia( i, valorRes );
			} else {
				r.setResistencia( estadoSel, valorRes );
			}
			refrescarTablaCon(r);
		}
		if (odgDePanelEdicionActual!=null) odgDePanelEdicionActual.getPanelEdicion();  // Refresca el panel con la resistencia (si procede)
	}
	
	// Método para poner resistencias partiendo de la configuración de la botonera de resistencias
	private void setResistencias( Resistencia... ress ) {
		hayCambiosEnCircuito = true;
		if (v.bR0.isSelected()) setResistencias( Resistencia.RESISTENCIA_CERO, ress );
		else if (v.bRInf.isSelected()) setResistencias( Resistencia.RESISTENCIA_INFINITA, ress );
		else if (v.bRVariable.isSelected()) {
			try {
				double d = Double.parseDouble( v.tfRVariable.getText() );
				setResistencias( d , ress );
			} catch (NumberFormatException exc) {
				v.setMensaje( Messages.getString("ControladorVentanaEAA.5") + v.tfRVariable.getText() ); //$NON-NLS-1$
			}
		}
	}
	
	//
	//
	// Combinaciones de interrupción
	//
	//
	
	protected void cambioCombInterrupcion() {
		redibujaCircuito();
		if (odgDePanelEdicionActual!=null) odgDePanelEdicionActual.getPanelEdicion();  // Refresca el panel con el voltaje (si procede)
	}

	// Gestión de componentes de manipulación en la ventana
	public void cambioEstadoTransparencia() {
		v.pDibujo.setTransparencia( v.slTransparencia.getValue() );
		redibujaCircuito();
	}

	public void cambioEstadoZoom() {
		logger.log( Level.FINEST, "Cambia zoom " + v.slZoom.getValue() ); //$NON-NLS-1$
		v.pDibujo.setZoom( v.slZoom.getValue() );
		redibujaCircuito();
	}

	public void dobleClickZoom() {
		v.slZoom.setValue( (VentanaEditorAveriaAuto.ZOOM_MAXIMO - VentanaEditorAveriaAuto.ZOOM_MINIMO) / 2 );
		logger.log( Level.FINEST, "Doble click zoom " + v.slZoom.getValue() ); //$NON-NLS-1$
		v.pDibujo.setZoom( v.slZoom.getValue() );
		redibujaCircuito();
	}

	public void clickZoom( String cuanto ) {
		logger.log( Level.FINEST, "Click zoom " + cuanto ); //$NON-NLS-1$
		int nuevoValor = v.slZoom.getValue();
		switch (cuanto) {
			case "-": { //$NON-NLS-1$
				nuevoValor -= 32;
				if (nuevoValor < VentanaEditorAveriaAuto.ZOOM_MINIMO) nuevoValor = VentanaEditorAveriaAuto.ZOOM_MINIMO;
			}
			case "+": { //$NON-NLS-1$
				nuevoValor += 32;
				if (nuevoValor > VentanaEditorAveriaAuto.ZOOM_MAXIMO) nuevoValor = VentanaEditorAveriaAuto.ZOOM_MAXIMO;
			}
			case "100": { //$NON-NLS-1$
				nuevoValor = (VentanaEditorAveriaAuto.ZOOM_MAXIMO + VentanaEditorAveriaAuto.ZOOM_MINIMO) / 2;
			}
		}
		v.slZoom.setValue( nuevoValor );
		v.pDibujo.setZoom( v.slZoom.getValue() );
		redibujaCircuito();
	}

	public void clickTrans( String cuanto ) {
		logger.log( Level.FINEST, "Click transparencia " + cuanto ); //$NON-NLS-1$
		int nuevoValor = v.slTransparencia.getValue();
		switch (cuanto) {
			case "-": { //$NON-NLS-1$
				nuevoValor -= 16;
				if (nuevoValor < 0) nuevoValor = 0;
				break;
			}
			case "+": { //$NON-NLS-1$
				nuevoValor += 16;
				if (nuevoValor > 255) nuevoValor = 255;
				break;
			}
			case "100": { //$NON-NLS-1$
				nuevoValor = 127;
				break;
			}
		}
		v.slTransparencia.setValue( nuevoValor );
		v.pDibujo.setTransparencia( v.slTransparencia.getValue() );
		redibujaCircuito();
	}
	
	public void cambioEdicionMenu( String cual ) {
		logger.log( Level.FINEST, "Cambio edición menú " + cual ); //$NON-NLS-1$
		v.mcbEdiComponente.setSelected( false ); v.mcbEdiConector.setSelected( false ); v.mcbEdiConexion.setSelected( false );
		v.mcbEdiMovto.setSelected( false ); v.mcbEdiPunto.setSelected( false ); v.mcbEdiSeleccion.setSelected( false );
		switch (cual) {
			case "Mov": { //$NON-NLS-1$
				v.mcbEdiMovto.setSelected( true );
				v.bMover.setSelected( true );
				break;
			}
			case "Sel": { //$NON-NLS-1$
				v.mcbEdiSeleccion.setSelected( true );
				v.bSeleccion.setSelected( true );
				break;
			}
			case "Pun": { //$NON-NLS-1$
				v.mcbEdiPunto.setSelected( true );
				v.bPunto.setSelected( true );
				break;
			}
			case "Conec": { //$NON-NLS-1$
				v.mcbEdiConector.setSelected( true );
				v.bConector.setSelected( true );
				break;
			}
			case "Conex": { //$NON-NLS-1$
				v.mcbEdiConexion.setSelected( true );
				v.bConexion.setSelected( true );
				break;
			}
			case "Comp": { //$NON-NLS-1$
				v.mcbEdiComponente.setSelected( true );
				v.bComponente.setSelected( true );
				break;
			}
		}
	}

	
	//
	// Ventana de configuración
	//

	// Cambio de texto en ayuda. Chequea corrección y avisa si fallo de formato
	public void focusLostCambioTextoAyuda() {
		if (checkFormatoAyuda(v.taConfigURLAyuda.getText())) {
			JOptionPane.showMessageDialog( null, Messages.getString("ControladorVentanaEAA.244") +  //$NON-NLS-1$
				Messages.getString("ControladorVentanaEAA.245") + //$NON-NLS-1$
				Messages.getString("ControladorVentanaEAA.246"), Messages.getString("ControladorVentanaEAA.247"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$ //$NON-NLS-2$
			v.ventConfiguracion.setVisible( true );
			v.ventConfiguracion.requestFocusInWindow();
			v.taConfigURLAyuda.requestFocus();
		}
	}
	
	// Devuelve true si hay error
	public boolean checkFormatoAyuda( String texto ) {
		StringTokenizer st = new StringTokenizer( texto, "\n" ); //$NON-NLS-1$
		while (st.hasMoreTokens()) {
			String linea = st.nextToken();
			StringTokenizer st2 = new StringTokenizer( linea, "," ); //$NON-NLS-1$
			if (!st2.hasMoreTokens()) return true; // No hay título
			st2.nextToken();  // Título
			if (!st2.hasMoreTokens()) return true; // No hay idioma
			String idioma = st2.nextToken();
			if (!"#ES##EN##EU##es##en##eu#".contains("#"+idioma+"#")) return true;  // Idioma incorrecto //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (!st2.hasMoreTokens()) return true; // No hay URL
		}
		return false;
	}

	// Cambio de texto en ayuda. Chequea corrección y avisa si fallo de formato
	public void focusLostCambioTextoAyudaElementos() {
		if (checkFormatoAyudaElementos(v.taConfigURLElementos.getText())) {
			JOptionPane.showMessageDialog( null, Messages.getString("ControladorVentanaEAA.253") +  //$NON-NLS-1$
				Messages.getString("ControladorVentanaEAA.254") + //$NON-NLS-1$
				Messages.getString("ControladorVentanaEAA.255"), Messages.getString("ControladorVentanaEAA.256"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$ //$NON-NLS-2$
			v.ventConfiguracion.setVisible( true );
			v.ventConfiguracion.requestFocusInWindow();
			v.taConfigURLElementos.requestFocus();
		}
	}
	
	// Devuelve true si hay error
	public boolean checkFormatoAyudaElementos( String texto ) {
		StringTokenizer st = new StringTokenizer( texto, "\n" ); //$NON-NLS-1$
		while (st.hasMoreTokens()) {
			String linea = st.nextToken();
			StringTokenizer st2 = new StringTokenizer( linea, "," ); //$NON-NLS-1$
			if (!st2.hasMoreTokens()) return true; // No hay elemento
			st2.nextToken();  // Título
			if (!st2.hasMoreTokens()) return true; // No hay idioma
			String idioma = st2.nextToken();
			if (!"#ES##EN##EU##es##en##eu#".contains("#"+idioma+"#")) return true;  // Idioma incorrecto //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return false;
	}
	
	public void reiniciarConfiguracion() {
		v.tfConfigAncho.setText(""); //$NON-NLS-1$
		v.tfConfigAlto.setText(""); //$NON-NLS-1$
		v.tfConfigTiempoSegs.setText(""); //$NON-NLS-1$
		v.tfConfigPuntuacionPartida.setText(""); //$NON-NLS-1$
		v.tfConfigNumMinMedidasVoltaje.setText(""); //$NON-NLS-1$
		v.tfConfigNumMaxMedidasVoltaje.setText(""); //$NON-NLS-1$
		v.tfConfigNumMinMedidasResistencia.setText(""); //$NON-NLS-1$
		v.tfConfigNumMaxMedidasResistencia.setText(""); //$NON-NLS-1$
		v.taConfigURLAyuda.setText(""); //$NON-NLS-1$
		// v.taConfigDescripcion.setText("");
		v.taConfigURLElementos.setText( VentanaEditorAveriaAuto.TEXTO_ESTANDAR_AYUDA_ELEMENTOS );
	}
	
	//
	//
	// Utilidades
	//
	//

	/** Redibuja el circuito
	 */
	public void redibujaCircuito() {
		v.pDibujo.repaint();
	}
	
	// Pide interactivamente un fichero existente de imagen
	// (null si no se selecciona)
	private File pedirFicheroImagen( String mens ) {
		File dirActual = new File( ultimoDirectorio );
		JFileChooser chooser = new JFileChooser( dirActual );
		chooser.setCurrentDirectory( dirActual );
		System.out.println( dirActual );
		chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		chooser.setFileFilter( new FileNameExtensionFilter( 
				Messages.getString("ControladorVentanaEAA.271"), "jpg", "png", "gif" ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		chooser.setDialogTitle( mens );
		int returnVal = chooser.showOpenDialog( null );
		if (returnVal == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile()!=null) {
			actualizarUltimoDir( chooser.getSelectedFile() );
			return chooser.getSelectedFile();
		} else 
			return null;
	}

	public static void actualizarUltimoDir( File f ) {
		ultimoDirectorio = f.getAbsolutePath();
		int ultimaContraBarra = ultimoDirectorio.lastIndexOf("\\"); //$NON-NLS-1$
		int ultimaBarra = ultimoDirectorio.lastIndexOf("/"); //$NON-NLS-1$
		ultimaBarra = Math.max( ultimaBarra, ultimaContraBarra );
		if (ultimaBarra >= 0) ultimoDirectorio = ultimoDirectorio.substring( 0, ultimaBarra );
	}


}
