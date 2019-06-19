package aem.averiasAuto;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.BevelBorder;

import aem.averiasAuto.elem.*;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class VentanaEditorAveriaAuto extends JFrame {
	
	protected static final String TEXTO_ESTANDAR_AYUDA_ELEMENTOS = "Fusible,ES,\nBatería,ES,\nLámpara,ES,\nPotenciómetro,ES,\nRelé,ES,\nInterruptor,ES,\nMotor,ES,\nBocina,ES,\nElectrónica,ES,"; //$NON-NLS-1$
	protected static final int ZOOM_MINIMO = 0;
	protected static final int ZOOM_MAXIMO = 1024;

	protected PanelDeDibujo pDibujo;

	protected JButton bConfig;
	protected JButton bGuardar, bCargar, bNuevo, bCalcCortes, bCargarGraf, bBorrar, bEditarAverias;  // Botones de acción de edición de circuito
	protected JButton bRecalcInterrupcion, bQuitaConectables, bPreCalculo, bEdiResistencias;  // Botones de acción de averías
	protected JButton bV12, bVVariable, bV0, bVPNC, bVInterm;
	protected JToggleButton bRInf, bRVariable, bR0;
	protected JTextField tfVVariable;
	protected JTextField tfRVariable;
	protected JToggleButton bMover, bSeleccion, bPunto, bConexion, bConector, bComponente;  // Botones de selección
	protected ButtonGroup bgBotonesSeleccion, bgBotonesSeleccionMenu;  // Grupos de botones
	protected JCheckBox cbAEjes;  // Checkbox para ajustar conexiones a los ejes (horizontal y vertical)
	protected JSlider slTransparencia;  // Slider de transparencia
	protected JSlider slZoom;  // Slider de zoom
	protected JScrollPane spConfig;  // Panel lateral de configuración
	protected JScrollPane spDibujo;  // Panel scroll de dibujo
	protected JLabel lMensaje;  // Label de mensaje
	protected JComboBox<String> cbComponentes;  // Combo de componente
	protected JComboBox<String> cbTiposElementos;  // Combo de tipos de elemento
	protected JList<CombinacionInterrupcion> lCombinacionesInterrupcion;  // Combo de combinaciones
		protected JScrollPane spCombinacionesInterrupcion; // Su scrollpane
	protected JSplitPane spCentral;  // Split pane central
	protected JTable tablaDerecha;  // JTable de información 
	protected JPanel pAtributos;    // Panel de edición de atributos
	protected JPanel pDerecho;      // Panel derecho de la ventana
	
	protected ControladorVentanaEAA miControlador = new ControladorVentanaEAA( this );
	
	protected JMenuItem miNuevo, miCargar, miGuardar, miCargarGraf, miConfig, miLastFile1, miLastFile2, miLastFile3, miLastFile4, miLastFile5;
	protected JMenuItem miZoomMenos, miZoomMas, miZoom100, miTransMenos, miTransMas, miTrans100, miMostrarCortes;
	protected JMenuItem miEliminar;
	protected JCheckBoxMenuItem mcbEdiMovto, mcbEdiSeleccion, mcbEdiPunto, mcbEdiConexion, mcbEdiConector, mcbEdiComponente;  // Checkboxes de menú
	
	private static ArrayList<VentanaEditorAveriaAuto> ventanasCreadas = new ArrayList<VentanaEditorAveriaAuto>();
	
	public ControladorVentanaEAA getControlador() { return miControlador; }
	
	/** Devuelve la última ventana creada
	 * @return	Última ventana creada
	 */
	public static VentanaEditorAveriaAuto getLastVentana() { if (ventanasCreadas.isEmpty()) return null; else return ventanasCreadas.get( ventanasCreadas.size()-1 ); }

	@Override
	public void dispose() {
		ventanasCreadas.remove( this );
		super.dispose();
	}

	public VentanaEditorAveriaAuto() {
		ventanasCreadas.add( this );
		// Configuración
		setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
		setSize( 800, 600 );
		setTitle( EditorAveriasAuto.VERSION_PROYECTO );
		
		// Datos y componentes
		lMensaje = new JLabel( " " ); //$NON-NLS-1$
		miControlador.init();
		JPanel pBotonera = new JPanel();
		cbAEjes = new JCheckBox( Messages.getString("VentanaEditorAveriaAuto.2") ); //$NON-NLS-1$
		bMover = new JToggleButton(); bMover.setIcon( new ImageIcon( getClass().getResource( "img/n_move.png" ))); bMover.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_move.png" ))); bMover.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_move.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			bMover.setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) );
		bSeleccion = new JToggleButton(); bSeleccion.setIcon( new ImageIcon( getClass().getResource( "img/n_seleccion.png" ))); bSeleccion.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_seleccion.png" ))); bSeleccion.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_seleccion.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			bSeleccion.setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) );
		bPunto = new JToggleButton(); bPunto.setIcon( new ImageIcon( getClass().getResource( "img/n_punto.png" ))); bPunto.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_punto.png" ))); bPunto.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_punto.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			bPunto.setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) );
		bConexion = new JToggleButton(); bConexion.setIcon( new ImageIcon( getClass().getResource( "img/n_conexion.png" ))); bConexion.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_conexion.png" ))); bConexion.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_conexion.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			bConexion.setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) );
		bConector = new JToggleButton(); bConector.setIcon( new ImageIcon( getClass().getResource( "img/n_conector.png" ))); bConector.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_conector.png" ))); bConector.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_conector.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			bConector.setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) );
		bComponente = new JToggleButton(); bComponente.setIcon( new ImageIcon( getClass().getResource( "img/n_componente.png" ))); bComponente.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_componente.png" ))); bComponente.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_componente.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			bComponente.setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) );
		cbComponentes = new JComboBox<String>( Componente.nombresComponentes );
		slTransparencia = new JSlider(0, 255);
		slZoom = new JSlider( ZOOM_MINIMO, ZOOM_MAXIMO );
		spDibujo = new JScrollPane( pDibujo );
		bgBotonesSeleccion = new ButtonGroup(); bgBotonesSeleccionMenu = new ButtonGroup();
		bConfig = new JButton(); bConfig.setIcon( new ImageIcon( getClass().getResource( "img/n_config.png" ))); bConfig.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_config.png" ))); bConfig.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_config.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		bGuardar = new JButton(); bGuardar.setIcon( new ImageIcon( getClass().getResource( "img/n_save.png" ))); bGuardar.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_save.png" ))); bGuardar.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_save.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		bCargar = new JButton(); bCargar.setIcon( new ImageIcon( getClass().getResource( "img/n_cargar.png" ))); bCargar.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_cargar.png" ))); bCargar.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_cargar.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		bNuevo = new JButton(); bNuevo.setIcon( new ImageIcon( getClass().getResource( "img/n_new.png" ))); bNuevo.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_new.png" ))); bNuevo.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_new.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		bCalcCortes = new JButton(); bCalcCortes.setIcon( new ImageIcon( getClass().getResource( "img/n_cortes.png" ))); bCalcCortes.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_cortes.png" ))); bCalcCortes.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_cortes.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		bCargarGraf = new JButton(); bCargarGraf.setIcon( new ImageIcon( getClass().getResource( "img/n_carga_grafico.png" ))); bCargarGraf.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_carga_grafico.png" ))); bCargarGraf.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_carga_grafico.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		bBorrar = new JButton(); bBorrar.setIcon( new ImageIcon( getClass().getResource( "img/n_delete.png" ))); bBorrar.setRolloverIcon( new ImageIcon( getClass().getResource( "img/r_delete.png" ))); bBorrar.setPressedIcon( new ImageIcon( getClass().getResource( "img/c_delete.png" ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		bEditarAverias = new JButton(Messages.getString("VentanaEditorAveriaAuto.42")); //$NON-NLS-1$
		bRecalcInterrupcion = new JButton(Messages.getString("VentanaEditorAveriaAuto.43")); //$NON-NLS-1$
		bQuitaConectables = new JButton(Messages.getString("VentanaEditorAveriaAuto.44")); //$NON-NLS-1$
		bPreCalculo = new JButton(Messages.getString("VentanaEditorAveriaAuto.45")); //$NON-NLS-1$
		bEdiResistencias = new JButton(Messages.getString("VentanaEditorAveriaAuto.46")); //$NON-NLS-1$
		cbTiposElementos = new JComboBox<String>( TipoElemento.getStrings() );
		((DefaultComboBoxModel<String>)(cbTiposElementos.getModel())).addElement( Messages.getString("VentanaEditorAveriaAuto.47") ); //$NON-NLS-1$
		lCombinacionesInterrupcion = new JList<CombinacionInterrupcion>();
		bV12 = new JButton( Messages.getString("VentanaEditorAveriaAuto.48") ); bV0 = new JButton( Messages.getString("VentanaEditorAveriaAuto.49") ); //$NON-NLS-1$ //$NON-NLS-2$
		bVVariable = new JButton( Messages.getString("VentanaEditorAveriaAuto.50") ); //$NON-NLS-1$
		bVPNC = new JButton( Messages.getString("VentanaEditorAveriaAuto.51") ); bVInterm = new JButton( Messages.getString("VentanaEditorAveriaAuto.52") ); //$NON-NLS-1$ //$NON-NLS-2$
		bRInf = new JToggleButton( Messages.getString("VentanaEditorAveriaAuto.53") ); bR0 = new JToggleButton( Messages.getString("VentanaEditorAveriaAuto.54") ); bRVariable = new JToggleButton( Messages.getString("VentanaEditorAveriaAuto.55") );  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		tfVVariable = new JTextField( "3.0" ); //$NON-NLS-1$
		tfRVariable = new JTextField( "5.0" ); //$NON-NLS-1$
		spCombinacionesInterrupcion = new JScrollPane(lCombinacionesInterrupcion);
				
		// Paneles
		pDerecho = new JPanel();
		JPanel pSelSuperior = new JPanel();
		JPanel pSeleccionTabla = new JPanel();
		spConfig = new JScrollPane();
		pAtributos = new JPanel();
		spCentral = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
		miControlador.initTabla();
		
		// Configuración componentes
		lMensaje.setHorizontalAlignment( JLabel.CENTER );
		slTransparencia.setPreferredSize( new Dimension( 80, 20 ));
		slZoom.setPreferredSize( new Dimension( 80, 20 ));
		cbAEjes.setSelected( false );
		bPunto.setSelected( true );
		spConfig.setPreferredSize( new Dimension( 300, 2000 ) ); 
		pDerecho.setLayout( new BorderLayout() );
		pSelSuperior.setLayout( new BorderLayout() );
		cbTiposElementos.setSelectedItem( Messages.getString("VentanaEditorAveriaAuto.58") ); //$NON-NLS-1$
		pAtributos.setLayout( new BorderLayout() );
		pBotonera.setLayout( new FlowLayout( FlowLayout.LEFT ));
		
		bRecalcInterrupcion.setVisible( false );
		bQuitaConectables.setVisible( false ); 
		bPreCalculo.setVisible( false );
		bEdiResistencias.setVisible( false );
		spCombinacionesInterrupcion.setVisible( false );
		bV0.setVisible( false ); bVVariable.setVisible( false ); bV12.setVisible( false ); bVInterm.setVisible( false ); bVPNC.setVisible(false);
		tfVVariable.setVisible( false );
		bR0.setVisible( false ); bRInf.setVisible( false ); bRVariable.setVisible( false );
		tfRVariable.setVisible( false );
		AbstractButton[] lBs = { bMover, bSeleccion, bPunto, bConexion, bConector, bComponente, bConfig, bGuardar, bCargar, bCalcCortes, bCargarGraf, 
				bBorrar, bEditarAverias, bV0, bVVariable, bV12, bVInterm, bVPNC, bRInf, bRVariable, bR0, bRecalcInterrupcion,
				bQuitaConectables, bPreCalculo, bEdiResistencias, bNuevo };
		for (AbstractButton bt : lBs ) bt.setMargin(new Insets(2, 1, 2, 1));
		lCombinacionesInterrupcion.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		// Hints
		bConfig.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.59") ); //$NON-NLS-1$
		bBorrar.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.60") ); //$NON-NLS-1$
		bGuardar.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.61") ); //$NON-NLS-1$
		bCargar.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.62") ); //$NON-NLS-1$
		bNuevo.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.63") ); //$NON-NLS-1$
		bCalcCortes.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.64") );  //$NON-NLS-1$
		bCargarGraf.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.65") ); //$NON-NLS-1$
		bEditarAverias.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.66") ); //$NON-NLS-1$
		
		bRecalcInterrupcion.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.67") ); //$NON-NLS-1$
		bQuitaConectables.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.68") ); //$NON-NLS-1$
		bPreCalculo.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.69") ); //$NON-NLS-1$
		bEdiResistencias.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.70") ); //$NON-NLS-1$
		
		bV12.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.71") ); //$NON-NLS-1$
		bVVariable.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.72") ); //$NON-NLS-1$
		bV0.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.73") ); //$NON-NLS-1$
		bVPNC.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.74") ); //$NON-NLS-1$
		bVInterm.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.75") ); //$NON-NLS-1$
		
		bRInf.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.76") ); //$NON-NLS-1$
		bRVariable.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.77") );  //$NON-NLS-1$
		bR0.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.78") ); //$NON-NLS-1$
		
		bMover.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.79") ); //$NON-NLS-1$
		bSeleccion.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.80") );  //$NON-NLS-1$
		bPunto.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.81") ); //$NON-NLS-1$
		bConexion.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.82") ); //$NON-NLS-1$
		bConector.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.83") ); //$NON-NLS-1$
		bComponente.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.84") ); //$NON-NLS-1$
		
		cbAEjes.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.85") ); //$NON-NLS-1$
		slTransparencia.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.86") ); //$NON-NLS-1$
		slZoom.setToolTipText( Messages.getString("VentanaEditorAveriaAuto.87") ); //$NON-NLS-1$
		
		// Añadir componentes a contenedores
		pBotonera.add( bConfig );
		pBotonera.add( bV12 ); pBotonera.add( bVVariable ); pBotonera.add( tfVVariable ); pBotonera.add( bV0 ); pBotonera.add( bVPNC ); pBotonera.add( bVInterm );
		pBotonera.add( bRInf ); pBotonera.add( bRVariable ); pBotonera.add( tfRVariable ); pBotonera.add( bR0 );
		pBotonera.add( bRecalcInterrupcion ); pBotonera.add( bQuitaConectables ); pBotonera.add( bPreCalculo );
		pBotonera.add( bBorrar );
		pBotonera.add( cbAEjes );
		pBotonera.add( bMover ); pBotonera.add( bSeleccion ); pBotonera.add( bPunto ); pBotonera.add( bConexion ); pBotonera.add( bConector ); pBotonera.add( bComponente );
		pBotonera.add( cbComponentes );
		pBotonera.add( new JLabel(Messages.getString("VentanaEditorAveriaAuto.88") ) ); pBotonera.add( slTransparencia ); //$NON-NLS-1$
		pBotonera.add( new JLabel(new ImageIcon( getClass().getResource( "img/n_zoom.png" ) )) ); pBotonera.add( slZoom ); //$NON-NLS-1$
		pBotonera.add( bGuardar );
		pBotonera.add( bCargar ); pBotonera.add( bNuevo ); pBotonera.add( bCalcCortes ); pBotonera.add( bCargarGraf );
		pBotonera.add( bEditarAverias ); pBotonera.add( bEdiResistencias );
		spConfig.setViewportView( tablaDerecha );
		bgBotonesSeleccion.add(bMover); bgBotonesSeleccion.add(bSeleccion); bgBotonesSeleccion.add(bPunto); bgBotonesSeleccion.add(bConexion); bgBotonesSeleccion.add(bConector); bgBotonesSeleccion.add(bComponente);
		pSeleccionTabla.add( cbTiposElementos );
		pSelSuperior.add( pSeleccionTabla, BorderLayout.SOUTH );
		pSelSuperior.add( spCombinacionesInterrupcion, BorderLayout.NORTH );
		pDerecho.add( pSelSuperior, BorderLayout.NORTH );
		pDerecho.add( spConfig, BorderLayout.CENTER );
		pDerecho.add( pAtributos, BorderLayout.SOUTH );
		spCentral.setLeftComponent( spDibujo );
		spCentral.setRightComponent( pDerecho );
		getContentPane().add( spCentral, BorderLayout.CENTER );
		getContentPane().add( pBotonera, BorderLayout.SOUTH );
		getContentPane().add( lMensaje, BorderLayout.NORTH );
		
		// Eventos
		pDibujo.addMouseListener( escuchadorPulsacionRaton );
		pDibujo.addMouseMotionListener( escuchadorMovtoRaton );
		slTransparencia.addChangeListener( new ChangeListener() { @Override
			public void stateChanged(ChangeEvent e) { miControlador.cambioEstadoTransparencia(); } });
		slZoom.addChangeListener( new ChangeListener() { @Override
			public void stateChanged(ChangeEvent e) { miControlador.cambioEstadoZoom();} });
		slZoom.addMouseListener( new MouseAdapter() { @Override
			public void mouseClicked(MouseEvent e) { if (e.getClickCount()==2) miControlador.dobleClickZoom();} });
		bConfig.addActionListener( new ActionListener() { 
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBConfig(); } });
		bGuardar.addActionListener( new ActionListener() { 
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBGuardar(); } });
		bEditarAverias.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBEditarAverias(); } });
		bRecalcInterrupcion.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBRecalcInterrupcion(); } });
		bQuitaConectables.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBQuitaConectables(); } });
		bPreCalculo.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBPreCalculo(); } });
		bEdiResistencias.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBEdiResistencias(); } });
		bV0.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBV0(); } });
		bVVariable.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBVVariable(); } });
		bV12.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBV12(); } });
		bVPNC.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBVPNC(); } });
		bVInterm.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBVInterm(); } });
		bRInf.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBRInf(); } });
		bRVariable.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBRVariable(); } });
		bR0.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBR0(); } });
		bCargar.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBCargar(); } });
		bNuevo.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBNuevo(); } });
		bCalcCortes.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBCalcCortes(); } });
		bCargarGraf.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBCargarGraf(); } });
		bBorrar.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { miControlador.clickBBorrar(); } });
		// bPuntoMed.addActionListener( new ActionListener() { @Override
		// 	public void actionPerformed(ActionEvent e) { cargarTabla(); }
		// });
		// bConexion.addActionListener( new ActionListener() { @Override
		// 	public void actionPerformed(ActionEvent e) { cargarTabla(); }
		// });
		// bConector.addActionListener( new ActionListener() { @Override
		// 	public void actionPerformed(ActionEvent e) { cargarTabla(); }
		// });
		// bComponente.addActionListener( new ActionListener() { @Override
		// 	public void actionPerformed(ActionEvent e) { cargarTabla(); }
		// });

		addWindowListener( new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) { miControlador.windowClosing(); } });
		
		cbComponentes.addItemListener( new ItemListener() { 
			@Override public void itemStateChanged(ItemEvent e) { miControlador.cbComponentesItem(); } });
		cbTiposElementos.addItemListener( new ItemListener() {
			@Override public void itemStateChanged(ItemEvent e) { miControlador.cbTiposElementosItem(); } });
		lCombinacionesInterrupcion.addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				miControlador.lCombinacionesInterrupcionItem();
			}
		});
		tablaDerecha.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			@Override public void valueChanged(ListSelectionEvent e) { miControlador.cambioSeleccionEnTabla( e ); } });
		tablaDerecha.getTableHeader().addMouseListener( new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (tablaDerecha.columnAtPoint( e.getPoint() ) == 0) miControlador.clickHeaderColumnaNombre();
			}
		});
		
		// Eventos - teclado
		pDibujo.setFocusable( true );
		Component[] lC = { pDibujo, cbAEjes, bMover, bSeleccion, bPunto, bConexion, bConector, bComponente, cbComponentes, slTransparencia,
				slZoom, spDibujo, bConfig, bGuardar, bCargar, bCalcCortes, bCargarGraf, bBorrar, bEditarAverias, bV0, bVVariable, bV12, bVInterm, bVPNC, tfVVariable, 
				bRInf, bRVariable, bR0, tfRVariable, bRecalcInterrupcion,
				bQuitaConectables, bPreCalculo, bEdiResistencias, cbTiposElementos, lCombinacionesInterrupcion, spConfig, tablaDerecha };
		for (Component c : lC ) c.addKeyListener( miControlador.escTeclado );
		for (Component c : lC ) c.addKeyListener( miControlador.escTecladoTextos );

		// Menús
		JMenuBar menuBar = new JMenuBar();
	    JMenu menuFichero = new JMenu( Messages.getString("VentanaEditorAveriaAuto.90") ); //$NON-NLS-1$
	    	miNuevo = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.91") ); menuFichero.add( miNuevo ); miNuevo.addActionListener( (e) -> { miControlador.clickBNuevo(); } ); //$NON-NLS-1$
	    	miCargar = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.92") ); menuFichero.add( miCargar ); miCargar.addActionListener( (e) -> { miControlador.clickBCargar(); } ); //$NON-NLS-1$
	    	miGuardar = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.93") ); menuFichero.add( miGuardar ); miGuardar.addActionListener( (e) -> { miControlador.clickBGuardar(); } ); //$NON-NLS-1$
	    menuFichero.addSeparator();
	    	miCargarGraf = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.94") ); menuFichero.add( miCargarGraf ); miCargarGraf.addActionListener( (e) -> { miControlador.clickBCargarGraf(); } ); //$NON-NLS-1$
	    menuFichero.addSeparator();
	    	miConfig = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.95") ); menuFichero.add( miConfig ); miConfig.addActionListener( (e) -> { miControlador.clickBConfig(); } ); //$NON-NLS-1$
	    menuFichero.addSeparator();
	    	miLastFile1 = new JMenuItem( "" ); menuFichero.add( miLastFile1 ); miLastFile1.addActionListener( (e) -> { miControlador.clickLastFile( 1 ); } ); //$NON-NLS-1$
	    	miLastFile2 = new JMenuItem( "" ); menuFichero.add( miLastFile2 ); miLastFile2.addActionListener( (e) -> { miControlador.clickLastFile( 2 ); } ); //$NON-NLS-1$
	    	miLastFile3 = new JMenuItem( "" ); menuFichero.add( miLastFile3 ); miLastFile3.addActionListener( (e) -> { miControlador.clickLastFile( 3 ); } ); //$NON-NLS-1$
	    	miLastFile4 = new JMenuItem( "" ); menuFichero.add( miLastFile4 ); miLastFile4.addActionListener( (e) -> { miControlador.clickLastFile( 4 ); } ); //$NON-NLS-1$
	    	miLastFile5 = new JMenuItem( "" ); menuFichero.add( miLastFile5 ); miLastFile5.addActionListener( (e) -> { miControlador.clickLastFile( 5 ); } ); //$NON-NLS-1$
	    menuBar.add( menuFichero );
	    JMenu menuVer = new JMenu( Messages.getString("VentanaEditorAveriaAuto.101") ); //$NON-NLS-1$
	    	miZoomMenos = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.102") ); menuVer.add( miZoomMenos ); miZoomMenos.addActionListener( (e) -> { miControlador.clickZoom( "-" ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    	miZoomMas = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.104") ); menuVer.add( miZoomMas ); miZoomMas.addActionListener( (e) -> { miControlador.clickZoom( "+" ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    	miZoom100 = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.106") ); menuVer.add( miZoom100 ); miZoom100.addActionListener( (e) -> { miControlador.clickZoom( Messages.getString("VentanaEditorAveriaAuto.107") ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    menuVer.addSeparator();
	    	miTransMenos = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.108") ); menuVer.add( miTransMenos ); miTransMenos.addActionListener( (e) -> { miControlador.clickTrans( "-" ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    	miTransMas = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.110") ); menuVer.add( miTransMas ); miTransMas.addActionListener( (e) -> { miControlador.clickTrans( "+" ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    	miTrans100 = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.112") ); menuVer.add( miTrans100 ); miTrans100.addActionListener( (e) -> { miControlador.clickTrans( Messages.getString("VentanaEditorAveriaAuto.113") ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    menuVer.addSeparator();
	    	miMostrarCortes = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.114") ); menuVer.add( miMostrarCortes ); miMostrarCortes.addActionListener( (e) -> { miControlador.clickBCalcCortes(); } ); //$NON-NLS-1$
	    menuBar.add( menuVer );
	    JMenu menuEdi = new JMenu( Messages.getString("VentanaEditorAveriaAuto.115") ); //$NON-NLS-1$
	    mcbEdiMovto = new JCheckBoxMenuItem( Messages.getString("VentanaEditorAveriaAuto.116") ); menuEdi.add( mcbEdiMovto ); mcbEdiMovto.addActionListener( (e) -> { miControlador.cambioEdicionMenu( "Mov" ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    mcbEdiSeleccion = new JCheckBoxMenuItem( Messages.getString("VentanaEditorAveriaAuto.118") ); menuEdi.add( mcbEdiSeleccion ); mcbEdiSeleccion.addActionListener( (e) -> { miControlador.cambioEdicionMenu( "Sel" ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    mcbEdiPunto = new JCheckBoxMenuItem( Messages.getString("VentanaEditorAveriaAuto.120") ); menuEdi.add( mcbEdiPunto ); mcbEdiPunto.addActionListener( (e) -> { miControlador.cambioEdicionMenu( "Pun" ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    mcbEdiConexion = new JCheckBoxMenuItem( Messages.getString("VentanaEditorAveriaAuto.122") ); menuEdi.add( mcbEdiConexion ); mcbEdiConexion.addActionListener( (e) -> { miControlador.cambioEdicionMenu( "Conex" ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    mcbEdiConector = new JCheckBoxMenuItem( Messages.getString("VentanaEditorAveriaAuto.124") ); menuEdi.add( mcbEdiConector ); mcbEdiConector.addActionListener( (e) -> { miControlador.cambioEdicionMenu( "Conec" ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    mcbEdiComponente = new JCheckBoxMenuItem( Messages.getString("VentanaEditorAveriaAuto.126") ); menuEdi.add( mcbEdiComponente ); mcbEdiComponente.addActionListener( (e) -> { miControlador.cambioEdicionMenu( "Comp" ); } ); //$NON-NLS-1$ //$NON-NLS-2$
	    bgBotonesSeleccionMenu.add( mcbEdiComponente ); bgBotonesSeleccionMenu.add( mcbEdiSeleccion ); bgBotonesSeleccionMenu.add( mcbEdiMovto ); 
	    bgBotonesSeleccionMenu.add( mcbEdiPunto ); bgBotonesSeleccionMenu.add( mcbEdiConexion ); bgBotonesSeleccionMenu.add( mcbEdiConector );
	    menuEdi.addSeparator();
	    	miEliminar = new JMenuItem( Messages.getString("VentanaEditorAveriaAuto.128") ); menuEdi.add( miEliminar ); miEliminar.addActionListener( (e) -> { miControlador.clickBBorrar(); } ); //$NON-NLS-1$
	    menuBar.add( menuEdi );
	    
	    setJMenuBar( menuBar );
		miControlador.actualizaMenuUltimosFicheros();  // Actualizar menú de ficheros últimos
		
		// Fin inicialización
		miControlador.initFinConstruccion();
	}

	public MarcasAverias getMarcasAverias() {
		return miControlador.marcasAverias;
	}

	public PanelDeDibujo getPanelDibujo() {
		return pDibujo;
	}

	/**
	 * @return	null si no hay seleccionada o no está en modo edición de averías
	 */
	public CombinacionInterrupcion getCombinacionSeleccionada() {
		if (lCombinacionesInterrupcion.isVisible() && lCombinacionesInterrupcion.getSelectedValue()!=null)
			return (CombinacionInterrupcion) lCombinacionesInterrupcion.getSelectedValue();
		else
			return null;
	}

	/**
	 * @return	-1 si no hay seleccionada o no está en modo edición de averías
	 */
	public int getIndiceCombinacionSeleccionada() {
		if (lCombinacionesInterrupcion.isVisible() && lCombinacionesInterrupcion.getSelectedValue()!=null)
			return lCombinacionesInterrupcion.getSelectedIndex();
		else
			return -1;
	}

	/**
	 * @return	-1 si no hay seleccionada o no está en modo edición de averías
	 */
	public boolean isCombinacionTodasSeleccionada() {
		if (lCombinacionesInterrupcion.isVisible() && lCombinacionesInterrupcion.getSelectedValue()!=null)
			return (lCombinacionesInterrupcion.getSelectedIndex() == miControlador.marcasAverias.getCombsInterrupcion().size());
		else
			return false;
	}

	// Escuchador principal de click y drag de ratón
	MouseListener escuchadorPulsacionRaton = new MouseAdapter() {
		public void mouseReleased(MouseEvent e) {
			miControlador.mouseReleased( e );
		}
		public void mousePressed(MouseEvent e) {
			miControlador.mousePressed( e );
		}
	};

	MouseMotionListener escuchadorMovtoRaton = new MouseMotionAdapter() {
		@Override
		public void mouseDragged(MouseEvent e) {
			miControlador.mouseDragged(e);
		}
	};

	public void setMensaje( String mens ) {
		lMensaje.setText( mens );
	}


	JTextField tfConfigAncho = new JTextField( 5 );
	JTextField tfConfigAlto = new JTextField( 5 );
	JTextField tfConfigTiempoSegs = new JTextField( 5 );
	JTextField tfConfigPuntuacionPartida = new JTextField( 5 );
	JTextField tfConfigNumMinMedidasVoltaje = new JTextField( 5 );
	JTextField tfConfigNumMaxMedidasVoltaje = new JTextField( 5 );
	JTextField tfConfigNumMinMedidasResistencia = new JTextField( 5 );
	JTextField tfConfigNumMaxMedidasResistencia = new JTextField( 5 );
	JTextArea taConfigURLAyuda = new JTextArea( 5, 60 );
	// JTextArea taConfigDescripcion = new JTextArea( 5, 60 );
	JTextArea taConfigURLElementos = new JTextArea( 5, 60 );
	VentanaConfig ventConfiguracion = new VentanaConfig();

	class VentanaConfig extends JDialog {
		public VentanaConfig() {
			setModal( true );
			setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
			setSize( 700, 450 );
			setLocationRelativeTo( VentanaEditorAveriaAuto.this );
			setTitle( Messages.getString("VentanaEditorAveriaAuto.129") ); //$NON-NLS-1$
			taConfigURLElementos.setText( TEXTO_ESTANDAR_AYUDA_ELEMENTOS );
			JPanel pPrincipal = new JPanel();
			pPrincipal.setLayout( new BoxLayout( pPrincipal, BoxLayout.Y_AXIS ));
				JPanel pLin = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
				pLin.add( new JLabel( Messages.getString("VentanaEditorAveriaAuto.130") ) ); //$NON-NLS-1$
				pLin.add( tfConfigAncho ); tfConfigAncho.setEditable( false );
				tfConfigAncho.setFocusable( false );
				pLin.add( new JLabel( Messages.getString("VentanaEditorAveriaAuto.131") ) ); //$NON-NLS-1$
				pLin.add( tfConfigAlto ); tfConfigAlto.setEditable( false );
				tfConfigAlto.setFocusable( false );
			pPrincipal.add( pLin );
				pLin = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
				pLin.add( new JLabel( Messages.getString("VentanaEditorAveriaAuto.132") ) ); //$NON-NLS-1$
				pLin.add( tfConfigTiempoSegs );
				pLin.add( new JLabel( Messages.getString("VentanaEditorAveriaAuto.133") ) ); //$NON-NLS-1$
				pLin.add( tfConfigPuntuacionPartida );
			pPrincipal.add( pLin );
				pLin = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
				pLin.add( new JLabel( Messages.getString("VentanaEditorAveriaAuto.134") ) ); //$NON-NLS-1$
				pLin.add( tfConfigNumMinMedidasVoltaje );
				pLin.add( new JLabel( Messages.getString("VentanaEditorAveriaAuto.135") ) ); //$NON-NLS-1$
				pLin.add( tfConfigNumMaxMedidasVoltaje );
			pPrincipal.add( pLin );
				pLin = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
				pLin.add( new JLabel( Messages.getString("VentanaEditorAveriaAuto.136") ) ); //$NON-NLS-1$
				pLin.add( tfConfigNumMinMedidasResistencia );
				pLin.add( new JLabel( Messages.getString("VentanaEditorAveriaAuto.137") ) ); //$NON-NLS-1$
				pLin.add( tfConfigNumMaxMedidasResistencia );
//			pPrincipal.add( pLin );
//				pLin = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
//				pLin.add( new JLabel( "Descripción y explicación del circuito:" ) );
//				pLin.add( new JScrollPane(taConfigDescripcion) );
			pPrincipal.add( pLin );
				pLin = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
				pLin.add( new JLabel( Messages.getString("VentanaEditorAveriaAuto.138") ) ); //$NON-NLS-1$
				pLin.add( new JScrollPane(taConfigURLAyuda) );
			pPrincipal.add( pLin );
				pLin = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
				pLin.add( new JLabel( Messages.getString("VentanaEditorAveriaAuto.139") ) ); //$NON-NLS-1$
				pLin.add( new JScrollPane(taConfigURLElementos) );
			pPrincipal.add( pLin );
				pLin = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
				JButton bOk = new JButton( Messages.getString("VentanaEditorAveriaAuto.140") ); //$NON-NLS-1$
				pLin.add( bOk );
			pPrincipal.add( pLin );
			getContentPane().add( pPrincipal, BorderLayout.CENTER );
			bOk.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setVisible( false );
				}
			});
			taConfigURLAyuda.addFocusListener( new FocusListener() {
				String valAnterior = null;
				@Override
				public void focusLost(FocusEvent e) {
					if (!taConfigURLAyuda.getText().equals( valAnterior )) {   // Cambio de texto: comprobar formato
						miControlador.focusLostCambioTextoAyuda();
					}
				}
				@Override
				public void focusGained(FocusEvent e) {
					valAnterior = taConfigURLAyuda.getText();
				}
			});
			taConfigURLElementos.addFocusListener( new FocusListener() {
				String valAnterior = null;
				@Override
				public void focusLost(FocusEvent e) {
					if (!taConfigURLElementos.getText().equals( valAnterior )) {   // Cambio de texto: comprobar formato
						miControlador.focusLostCambioTextoAyudaElementos();
					}
				}
				@Override
				public void focusGained(FocusEvent e) {
					valAnterior = taConfigURLElementos.getText();
				}
			});
		}
	}

}

