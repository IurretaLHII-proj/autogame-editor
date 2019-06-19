package aem.averiasAuto.elem;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;

import java.util.ArrayList;
import java.util.StringTokenizer;

import aem.averiasAuto.MarcasAverias;
import aem.averiasAuto.TextoDeCircuito;
import aem.averiasAuto.VentanaEditorAveriaAuto;

// TODO solo soporta intermitencia de 5 décimas
public class Punto extends Point implements ObjetoDeGrafico, Comparable<Punto> {
	private static final long serialVersionUID = 1L;
	private String nombre; // Nombre del punto
	private boolean conectable;  // El punto es conectable o no (true por defecto)
	private ArrayList<java.lang.Double> voltaje;         // Lista de voltajes de este punto (para cada estado del circuito)
	private ArrayList<Boolean> voltajeEditado;  // Lista de información de voltajes editados de este punto (para cada estado del circuito) (true si ha sido editado, false si se ha calculado automáticamente)
	public static final double VOLTAJE_INDEFINIDO = -55;
	public static final double VOLTAJE_PNC = -56;   // Punto No Contacto
	public static final double VOLTAJE_INTERMITENTE1000 = -105;  // Voltaje intermitente cada 5 décimas 
	public static final double VOLTAJE_MINIMO = 0;  // Voltaje mínimo
	public static final double VOLTAJE_MAXIMO = 12;  // Voltaje máximo
	private static final int RADIO_CIRCULO_VOLTAJE = 10;
	private static final double VOLTAJE_MAXIMO_COLOR = 12;
	private static final int COLOR_RMAX = 255;
	private static final int COLOR_RMIN = 0;
	private static final int COLOR_GMAX = 0;
	private static final int COLOR_GMIN = 0;
	private static final int COLOR_BMAX = 0;
	private static final int COLOR_BMIN = 0;
	private static final Color COLOR_INDEFINIDO = Color.lightGray;
	private static final Color COLOR_PNC = Color.magenta;
	private static final Color COLOR_INTERMITENTE = Color.orange;

	public Punto( int x, int y, String nom ) {
		super(x,y);
		setNombre( nom );
		conectable = true;
		voltaje = new ArrayList<java.lang.Double>();
		voltajeEditado = new ArrayList<Boolean>();
	}
	
	// Constructor privado - no comprueba si hay nombres duplicados. Para clones de hacer-deshacer
	private Punto( int x, int y ) {
		super(x,y);
		conectable = true;
		voltaje = new ArrayList<java.lang.Double>();
		voltajeEditado = new ArrayList<Boolean>();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		Punto p = new Punto( x, y );
		p.nombre = nombre;
		p.conectable = conectable;
		p.voltaje = (ArrayList<java.lang.Double>) voltaje.clone();
		p.voltajeEditado = (ArrayList<Boolean>) voltajeEditado.clone();
		p.textoAsociado = textoAsociado;
		return p;
	}

	/** Convierte el objeto a String en una línea -con atributos separados por comas-
	 * @return
	 */
	public String aTexto() {
		String ret = x + "," + y + "," + nombre + "," + conectable;
		int posi=0;
		for (java.lang.Double v : voltaje) {
			boolean editado = voltajeEditado.get(posi); posi++;
			String stEditado = editado ? "" : "a";
			if (v == VOLTAJE_INDEFINIDO) ret += ("," + stEditado + "IND");
			else if (v == VOLTAJE_PNC) ret += ("," + stEditado + "PNC");
			else if (v == VOLTAJE_INTERMITENTE1000) ret += ("," + stEditado + "I1000");
			else ret += ("," + stEditado + v.doubleValue());
		}
		return ret;
	}

	/** Convierte los datos de texto del String en los atributos del punto.
	 * Si hay algún error no construye correctamente el punto y devuelve NullPointer
	 * @param s
	 */
	public Punto( String s ) throws NullPointerException {
		super(0,0);
		s = s.trim();
		StringTokenizer st = new StringTokenizer( s, "," );
		if (st.countTokens()>=4) {
			try {	
				String tok = st.nextToken();
				x = Integer.parseInt( tok );
				tok = st.nextToken();
				y = Integer.parseInt( tok );
				setNombre( st.nextToken() );
				tok = st.nextToken();
				conectable = Boolean.parseBoolean( tok );
				voltaje = new ArrayList<java.lang.Double>();
				voltajeEditado = new ArrayList<Boolean>();
				while (st.hasMoreTokens()) {
					tok = st.nextToken();
					if (tok.startsWith("a")) {
						tok = tok.substring(1);
						voltajeEditado.add( false );
					} else {
						voltajeEditado.add( true );
					}
					if (tok.equals("IND") || tok.equals("INDEF")) {
						voltaje.add( VOLTAJE_INDEFINIDO );
					} else if (tok.equals("PNC")) {
						voltaje.add( VOLTAJE_PNC );
					} else if (tok.toUpperCase().startsWith("I")) {
						voltaje.add( VOLTAJE_INTERMITENTE1000 );  
					} else {
						double volt = java.lang.Double.parseDouble( tok );
						voltaje.add( volt );
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new NullPointerException( "Error en construcción de objeto Punto desde texto " + s ); 
			}
		}
	}
	
	public TipoElemento getTipoElemento() {
		return TipoElemento.Punto;
	}
	
	public String getNombre() {
		return nombre;
	}
	/** Pone el nombre quitando comas, retornos de carro o tabuladores, comprobando que el nombre sea correcto (pidiendo interactivamente un nuevo nombre en caso contrario)
	 * @param nombre
	 */
	public void setNombre(String nombre) {
		boolean correcto = true;
		nombre = nombre.replaceAll( "\t", "" ).replaceAll( "\n", "" ).replaceAll( ",", "" );
		this.nombre = nombre;
		if (VentanaEditorAveriaAuto.getLastVentana() != null) {
			correcto = VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().chequearNombrePuntoCorrecto(this);
		}
		if (correcto) {  // Si no es correcto el propio método de chequeo hace una llamada a setNombre, sobra repetir esto
			if (textoAsociado!=null) {
				textoAsociado.setText( this.nombre );
			}
			if (tfNombre!=null) {
				tfNombre.setText( this.nombre );
			}
		}
	}
	public boolean isConectable() {
		return conectable;
	}
	public void setConectable(boolean conectable) {
		this.conectable = conectable;
		if (cbConectable!=null) cbConectable.setSelected( conectable );
	}
	
	public ArrayList<java.lang.Double> getVoltajes() { return voltaje; }
	public ArrayList<Boolean> getVoltajesEditados() { return voltajeEditado; }
	public java.lang.Double getVoltaje( int numEstadoCircuito ) { return voltaje.get(numEstadoCircuito); }
	public boolean getVoltajeEditado( int numEstadoCircuito ) { return voltajeEditado.get(numEstadoCircuito); }
	public void initVoltajes( int numEstadosCircuito ) {
		voltaje = new ArrayList<java.lang.Double>();
		voltajeEditado = new ArrayList<Boolean>();
		for (int i=0; i<numEstadosCircuito; i++) voltaje.add( VOLTAJE_INDEFINIDO );
		for (int i=0; i<numEstadosCircuito; i++) voltajeEditado.add( false );
	}
	public void setVoltaje( int numEstadoCircuito, double volt ) {
		voltaje.set( numEstadoCircuito, volt );
		voltajeEditado.set( numEstadoCircuito, true );
		actualizaVoltajeEnPanelEdicion();
	}
	public void setVoltajeEditado( int numEstadoCircuito, boolean editado ) {
		voltajeEditado.set( numEstadoCircuito, editado );
	}
	
	/** Devuelve información sobre si el voltaje está o no definido
	 * @param numEstadoCircuito	Número de estado de combinación del punto
	 * @return	true si el voltaje está definido, false en caso contrario
	 */
	public boolean isVoltajeDefinido( int numEstadoCircuito ) {
		return voltaje.get(numEstadoCircuito)>=VOLTAJE_MINIMO && voltaje.get(numEstadoCircuito)<=VOLTAJE_MAXIMO;
	}
	/** Devuelve información sobre si el voltaje es o no PNC (Punto No Contacto)
	 * @param numEstadoCircuito	Número de estado de combinación del punto
	 * @return	true si el voltaje es PNC, false en caso contrario
	 */
	public boolean isVoltajePNC( int numEstadoCircuito ) {
		return voltaje.get(numEstadoCircuito)==VOLTAJE_PNC;
	}
	/** Devuelve información sobre si el voltaje es o no intermitente
	 * @param numEstadoCircuito	Número de estado de combinación del punto
	 * @return	true si el voltaje es intermitente, false en caso contrario
	 */
	public boolean isVoltajeIntermitente( int numEstadoCircuito ) {
		return voltaje.get(numEstadoCircuito)==VOLTAJE_INTERMITENTE1000;
	}

	@Override
	public Object getVal(String prop) {
		prop = prop.toUpperCase();
		if (prop.equals("NOMBRE")) return nombre;
		else if (prop.equals("CONECTABLE")) return new Boolean(conectable);
		else if (prop.equals("X")) return new Integer(x);
		else if (prop.equals("Y")) return new Integer(y);
		else return null;
	}

	@Override
	public void setVal(String prop, Object val) {
		prop = prop.toUpperCase();
		try {
			if (prop.equals("NOMBRE")) setNombre( (String) val );
			else if (prop.equals("CONECTABLE")) conectable = (Boolean) val;
			else if (prop.equals("X")) x = (Integer) val;
			else if (prop.equals("Y")) y = (Integer) val;
		} catch (Exception e) {
			// Asignación incorrecta
			System.out.println( "Error en setVal - " + prop + " = " + val + " --> " + e.getMessage() );
		}
	}
	
	// Dos puntos son iguales si sus nombres lo son. Un punto es igual a un String si el String es su nombre
	@Override
	public boolean equals(Object o) {
		if (o instanceof Punto) {
			Punto p2 = (Punto)o;
			if (nombre == null) return p2.nombre==null;
			return (nombre.equals(p2.nombre));
		} else if (o instanceof String) {
			String n = (String)o;
			if (nombre == null) return n==null;
			return (nombre.equals(n));
		} else return false;
	}

		private static String[] props = { "NOMBRE", "CONECTABLE", "X", "Y" };
		private static Class<?>[] tipos = { String.class, Boolean.class, Integer.class, Integer.class };
		
	public String[] getProps() { return props; }
	public Class<?>[] getTipos() { return tipos; }

	public String toString() { return "<" + nombre + " " + x + "," + y + ">"; }
	
	public String toDescripcion() { return "<" + x + "," + y + ">"; }

	@Override
	public int compareTo(Punto arg0) {
		if (this==arg0) return 0;
		if (this.nombre==null) return -1;
		if (arg0.nombre==null) return +1;
		return (this.nombre.compareTo( arg0.nombre ));
	}

	/** Dibuja el objeto en el objeto graphics indicado
	 * @param g
	 */
	public void dibuja( Graphics2D g, Color col, Stroke st, double zoom ) {
		g.setStroke( st );
		if (!VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().isModoEdicion()) {  // Si está en modo avería... dibujar el voltaje
			if (VentanaEditorAveriaAuto.getLastVentana().isCombinacionTodasSeleccionada()) {   // Mirar si hay un voltaje único para todos los estados
				double vComun = getVoltajeFromTodos();
				if (vComun!=-1) {
					setColorFromVoltaje(g, vComun);
					MarcasAverias.dibujaOvalo( g, this, RADIO_CIRCULO_VOLTAJE, zoom );
				}
			} else {
				int estado = VentanaEditorAveriaAuto.getLastVentana().getIndiceCombinacionSeleccionada();
				if (estado!=-1 && estado<voltaje.size()) {
					setColorFromVoltaje( g, voltaje.get(estado) );
					MarcasAverias.dibujaOvalo( g, this, RADIO_CIRCULO_VOLTAJE, zoom );
				}
			}
		}
		g.setColor( col );
		MarcasAverias.dibujaPunto( g, this, zoom );
	}
		// Devuelve el voltaje del punto para todos sus estados, -1 si no hay un voltaje común 
		private double getVoltajeFromTodos() {
			double vComun = -1;
			for (double v : voltaje) {
				if (vComun == -1) vComun = v;
				else if (vComun != v) { vComun = -1; break; }
			}
			return vComun;
		}
		private void setColorFromVoltaje( Graphics2D g, double v ) {
			if (v==VOLTAJE_INDEFINIDO)
				g.setColor( COLOR_INDEFINIDO );
			else if (v==VOLTAJE_PNC)
				g.setColor( COLOR_PNC );
			else if (v==VOLTAJE_INTERMITENTE1000)
				g.setColor( COLOR_INTERMITENTE );
			else {
				double graduacion = v / VOLTAJE_MAXIMO_COLOR;
				g.setColor( new Color( (int)(COLOR_RMIN + (COLOR_RMAX-COLOR_RMIN)*graduacion), (int)(COLOR_GMIN + (COLOR_GMAX-COLOR_GMIN)*graduacion), (int)(COLOR_BMIN + (COLOR_BMAX-COLOR_BMIN)*graduacion)));
			}
		}

	private transient TextoDeCircuito textoAsociado = null;
	public void setTextoAsociado( TextoDeCircuito tdc ) { textoAsociado = tdc; }
	public TextoDeCircuito getTextoAsociado() { return textoAsociado; }
	
	private transient JPanel miPanelEdicion = null;
	private transient JTextField tfNombre = null;
	private transient JCheckBox cbConectable = null;
	private transient JLabel lVoltaje = null;
	private transient JTextField tfVoltaje = null;
	@Override
	public JPanel getPanelEdicion() {
		if (miPanelEdicion == null) {
			miPanelEdicion = new JPanel();
			miPanelEdicion.setMinimumSize( new Dimension( 100, 150 ));
			JPanel linea1 = new JPanel();
			JPanel linea2 = new JPanel();
			JPanel linea3 = new JPanel();
			miPanelEdicion.setLayout( new BoxLayout( miPanelEdicion, BoxLayout.Y_AXIS ));
			miPanelEdicion.add( linea1 );
			miPanelEdicion.add( linea2 );
			miPanelEdicion.add( linea3 );
			linea1.add( new JLabel( "Nombre: ") );
				tfNombre = new JTextField( 10 );
				tfNombre.setText( nombre );
				linea1.add( tfNombre );
			cbConectable = new JCheckBox( "¿Conectable?");
				linea2.add( cbConectable );
				cbConectable.setSelected( conectable );
			lVoltaje = new JLabel( "Voltaje:" );
			linea3.add( lVoltaje );
				tfVoltaje = new JTextField( 5 );
				tfVoltaje.setVisible( false );
				lVoltaje.setVisible( false );
				linea3.add( tfVoltaje );
			// Eventos
			tfNombre.addFocusListener( new FocusListener() {
				String antVal = null;
				@Override
				public void focusLost(FocusEvent e) {
					if (!tfNombre.getText().equals(antVal)) {  // Ha habido cambio de valor en el textfield
						String nuevoNombre = tfNombre.getText();
						setNombre( nuevoNombre );
					}
				}
				@Override
				public void focusGained(FocusEvent e) {
					antVal = tfNombre.getText();
				}
			});
			cbConectable.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					conectable = cbConectable.isSelected();
					System.out.println( "Check: " + conectable + " --> " + cbConectable.hashCode() );
				}
			});
			tfVoltaje.addFocusListener( new FocusAdapter() {
				String valAnterior = null;
				@Override public void focusGained(FocusEvent e) {
					valAnterior = tfVoltaje.getText();
				}
				@Override public void focusLost(FocusEvent e) {
					if (!tfVoltaje.getText().equals(valAnterior))
						actualizaVoltajeDesdePanelEdicion( tfVoltaje.getText() );
				}
			});
			Component[] lC = { tfNombre, cbConectable, tfVoltaje };
			for (Component c : lC ) c.addKeyListener( VentanaEditorAveriaAuto.getLastVentana().getControlador().escTeclado );
			for (Component c : lC ) c.addKeyListener( VentanaEditorAveriaAuto.getLastVentana().getControlador().escTecladoTextos );
		} else {
			tfNombre.setText( nombre );
			cbConectable.setSelected( conectable );
		}
		actualizaVoltajeEnPanelEdicion();
		return miPanelEdicion;
	}

		public void actualizaVoltajeDesdePanelEdicion( String valEnPanel ) {
			if (!VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().isModoEdicion()) {
				String texVol = valEnPanel.toUpperCase();
				if (VentanaEditorAveriaAuto.getLastVentana().isCombinacionTodasSeleccionada()) {
					try {
						double d = 0;
						if (texVol.equals("IND") || texVol.equals("INDEF")) d = VOLTAJE_INDEFINIDO;
						else if (texVol.equals("INT5") || texVol.equals("INT")) d = VOLTAJE_INTERMITENTE1000;
						else if (texVol.equals("PNC")) d = VOLTAJE_PNC;
						else d = java.lang.Double.parseDouble( texVol );
						for (int i=0; i<voltaje.size(); i++) {
							voltaje.set( i, d ); voltajeEditado.set( i, true );
						}
					} catch (NumberFormatException ex) {
					}
				} else {
					int indi = VentanaEditorAveriaAuto.getLastVentana().getIndiceCombinacionSeleccionada();
					if (indi!=-1 && indi<voltaje.size()) {
						try {
							double d = 0;
							if (texVol.equals("IND")) d = VOLTAJE_INDEFINIDO;
							else if (texVol.equals("INT5") || texVol.equals("INT")) d = VOLTAJE_INTERMITENTE1000;
							else if (texVol.equals("PNC")) d = VOLTAJE_PNC;
							else d = java.lang.Double.parseDouble( texVol );
							voltaje.set( indi, d ); voltajeEditado.set( indi, true );
						} catch (NumberFormatException ex) {
						}
					}
				}
			}
		}
		private void actualizaVoltajeEnPanelEdicion() {
			if (tfVoltaje!=null && !VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().isModoEdicion()) {
				if (VentanaEditorAveriaAuto.getLastVentana().isCombinacionTodasSeleccionada()) {
					double vComun = getVoltajeFromTodos();
					if (vComun==-1)
						tfVoltaje.setText( "VARIOS" );
					else if (vComun==VOLTAJE_INDEFINIDO)
						tfVoltaje.setText( "INDEF" );
					else if (vComun==VOLTAJE_INTERMITENTE1000)
						tfVoltaje.setText( "INT5" );
					else if (vComun==VOLTAJE_PNC)
						tfVoltaje.setText( "PNC" );
					else
						tfVoltaje.setText( vComun+"" );
				} else {
					int indi = VentanaEditorAveriaAuto.getLastVentana().getIndiceCombinacionSeleccionada();
					if (indi!=-1 && indi<voltaje.size()) {
						if (voltaje.get(indi)==VOLTAJE_INDEFINIDO)
							tfVoltaje.setText( "INDEF" );
						else if (voltaje.get(indi)==VOLTAJE_INTERMITENTE1000)
							tfVoltaje.setText( "INT5" );
						else if (voltaje.get(indi)==VOLTAJE_PNC)
							tfVoltaje.setText( "PNC" );
						else 
							tfVoltaje.setText( voltaje.get(indi)+"" );
						tfVoltaje.setVisible(true);
					}
				}
			}
		}
		
	
	
	/** Comprueba que el nombre del punto sea estándar: empieza por P y después tiene un número natural
	 * @param pto	Punto
	 * @return
	 */
	public boolean nombrePuntoEstandar() {
		String nom = getNombre().toUpperCase();
		if (nom.startsWith("P")) {
			for (int i=1; i<nom.length(); i++) {
				char c = nom.charAt(i);
				if (c<'0' || c>'9') return false;
			}
			return true;
		} else return false;
	}

}
