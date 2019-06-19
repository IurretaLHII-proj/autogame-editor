package aem.averiasAuto.elem;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;
import javax.swing.border.StrokeBorder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import aem.averiasAuto.MarcasAverias;
import aem.averiasAuto.PanelDeDibujo;
import aem.averiasAuto.VentanaEditorAveriaAuto;

public class Resistencia extends Conexion {
	private static final long serialVersionUID = 1L;
	public static final double RESISTENCIA_INFINITA = Double.MAX_VALUE;
	public static final double RESISTENCIA_INDEFINIDA = -5.0;
	public static final double RESISTENCIA_CERO = 0;
	private static final Color COLOR_INDEFINIDA = Color.lightGray;
	private static final Color COLOR_NOVISUALIZABLE = Color.magenta;
	private static final Color COLOR_VALOR = Color.blue;
	private static Color COLOR_RESISTENCIA = new Color( 0, 0, 240, 60);  // Color rojo con transparencia
	private static Stroke STROKE_RESISTENCIA = new BasicStroke(11);
	private static Stroke STROKE_RESISTENCIA_SELEC = new BasicStroke(17);

	private ArrayList<java.lang.Double> resistencia;  // Lista de valores de resistencia de esta resistencia (para cada estado del circuito)
	private ArrayList<Boolean> resistenciaEditada;    // Lista de informaciones de si el valor de la resistencia está editada (para cada estado del circuito)
	
	public Resistencia() {
		super();
		resistencia = new ArrayList<java.lang.Double>();
		resistenciaEditada = new ArrayList<Boolean>();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		Resistencia res = new Resistencia();
		res.listaPuntos = (ArrayList<Punto>) listaPuntos.clone();
		res.nombre = nombre;
		res.resistencia = (ArrayList<java.lang.Double>) resistencia.clone();
		res.resistenciaEditada = (ArrayList<Boolean>) resistenciaEditada.clone();
		return res;
	}

	/** Convierte el objeto a String en una línea -con atributos separados por comas-
	 * @return	Versión textual del objeto
	 */
	public String aTexto() {
		String ret = nombre + ",RESISTENCIA";
		for (Punto p : listaPuntos) {
			ret += ("," + p.getNombre());
		}
		int posi=0;
		for (java.lang.Double r : resistencia) {
			boolean editada = resistenciaEditada.get(posi); posi++;
			String stEditada = (editada) ? "" : "a";
			if (r == RESISTENCIA_INFINITA) ret += ("," + stEditada + "INFINITA");
			else if (r == RESISTENCIA_INDEFINIDA) ret += ("," + stEditada + "IND");
			else ret += ("," + stEditada + r.doubleValue());
		}
		return ret;
	}

	/** Convierte los datos de texto del String en los atributos de la resistencia.
	 * Si hay algún error no construye correctamente la resistencia y devuelve NullPointer
	 * @param s	String de la resistencia en formato texto
	 * @param listaPuntos	Lista de todos los puntos (debe incluir los puntos nombrados en la resistencia)
	 * @return	Nueva resistencia
	 */
	public static Resistencia crearResistencia( String s, ArrayList<Punto> listaPuntos ) throws NullPointerException {
		Resistencia ret = null;
		s = " " + s.trim();
		StringTokenizer st = new StringTokenizer( s, "," );
		if (st.countTokens()>=4) {
			try {
				ret = new Resistencia();
				ret.nombre = st.nextToken().trim();
				st.nextToken();  // token = "RESISTENCIA"
				String nom = "";
				for (int i=0; i<2; i++) {  // Itera los 2 puntos de la resistencia
					nom = st.nextToken();
					int posi = listaPuntos.size()-1; while (posi>=0 && !listaPuntos.get(posi).equals(nom)) posi--;
					if (posi==-1) throw new NullPointerException("Error en construcción de objeto Resistencia desde texto (punto " + nom + " no existente)");
					ret.add( listaPuntos.get(posi) ); 
				}
				ret.resistencia = new ArrayList<java.lang.Double>();
				ret.resistenciaEditada = new ArrayList<Boolean>();
				while (st.hasMoreTokens()) {
					nom = st.nextToken();
					if (nom.startsWith("a")) {
						nom = nom.substring(1);
						ret.resistenciaEditada.add( false );
					} else {
						ret.resistenciaEditada.add( true );
					}
					if (nom.equals("INFINITA")) {
						ret.resistencia.add( RESISTENCIA_INFINITA );
					} else if (nom.equals("IND")) {
						ret.resistencia.add( RESISTENCIA_INDEFINIDA );
					} else {
						double res = java.lang.Double.parseDouble( nom );
						ret.resistencia.add( res );
					}
				}
			} catch (NoSuchElementException e) {
				throw new NullPointerException( "Error en construcción de objeto Resistencia desde texto " + s ); 
			}
		} 
		if (ret == null) throw new NullPointerException( "Error en construcción de objeto Resistencia desde texto " + s );
		return ret;
	}

	public TipoElemento getTipoElemento() {
		return TipoElemento.Resistencia;
	}
	
	// Métodos heredados
	// public String getNombre()
	// public void setNombre(String nombre)
	// public Punto get( int index ) { return listaPuntos.get( index ); }
	// public void add( Punto p ) { listaPuntos.add( p ); }
	// public int size() { return listaPuntos.size(); }
	// public ArrayList<Punto> getAL() { return listaPuntos; }
	// public Object getVal(String prop)
	// public void setVal(String prop, Object val) {
	// public String[] getProps() { return props; }
	// public Class<?>[] getTipos() { return tipos; }

	public String toString() { return "RS " + nombre + "(RESISTENCIA) -> " + listaPuntos; }
	
	public String toDescripcion() { 
		String listaPtos = "{";
		for (Punto p : listaPuntos)
			listaPtos = listaPtos + p.getNombre() + ",";
		if (listaPuntos.size()>0) listaPtos = listaPtos.substring( 0, listaPtos.length()-1 ) + "}";
		return "RS -> " + listaPtos + " " + toStringResistencias(); 
	}

	// Dos resistencias son iguales si sus puntos son iguales, y además si sus nombres lo son
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (o instanceof Resistencia) {
			Resistencia r2 = (Resistencia)o;
			ArrayList<Punto> puntos1 = (ArrayList<Punto>) getAL().clone();
			ArrayList<Punto> puntos2 = (ArrayList<Punto>) r2.getAL().clone();
			Collections.sort( puntos1 ); Collections.sort( puntos2 );
			if (!puntos1.equals(puntos2)) return false;  // Si los puntos no son los mismos, no son iguales
			if (nombre == null) return r2.nombre==null;
			if (nombre.equals("")) return r2.nombre.equals("");
			return (nombre.equals(r2.nombre));
		} else return false;
	}

	/** Dibuja el objeto en el objeto graphics indicado
	 * @param g
	 */
	public void dibuja( Graphics2D g, Color col, Stroke st, double zoom ) {
		if (VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().isModoResistencias()) {  // Si está en modo resistencia... dibujar la resistencia
			if (listaPuntos.size()!=2) return;  // La resistencia debería tener siempre 2 puntos
			Point pIni = listaPuntos.get(0);
			Point pFin = listaPuntos.get(1);
			g.setColor( COLOR_RESISTENCIA ); g.setStroke( STROKE_RESISTENCIA );
			MarcasAverias.dibujaLinea( g, pIni, pFin, zoom );
			g.setColor( col ); g.setStroke( st );
			if (st.equals( MarcasAverias.STROKE_SELECCION )) g.setStroke( STROKE_RESISTENCIA_SELEC );
			Point pMed = new Point( (pIni.x+pFin.x)/2, (pIni.y+pFin.y)/2 );  // Punto intermedio para dibujado
			Point p1 = new Point( pIni.x+(pFin.x-pIni.x)/3*1, pIni.y+(pFin.y-pIni.y)/3*1 );  // Punto intermedio para dibujado
			Point p2 = new Point( pIni.x+(pFin.x-pIni.x)/3*2, pIni.y+(pFin.y-pIni.y)/3*2 );  // Punto intermedio para dibujado
			double resist = -1;
			if (VentanaEditorAveriaAuto.getLastVentana().isCombinacionTodasSeleccionada()) {   // Mirar si hay una resistencia única para todos los estados
				double rComun = getResistenciaFromTodos();
				resist = rComun;
			} else {
				int estado = VentanaEditorAveriaAuto.getLastVentana().getIndiceCombinacionSeleccionada();
				if (estado!=-1 && estado<resistencia.size()) {
					resist = resistencia.get(estado);
				}
			}
			if (resist==-1) {  // Resistencia no representable: círculos de color
				MarcasAverias.dibujaLinea( g, pIni, pFin, zoom );
				g.setColor( COLOR_NOVISUALIZABLE );
				MarcasAverias.dibujaCirculo( g, p1, MarcasAverias.RADIO_MARCA_RESISTENCIA, zoom );
				MarcasAverias.dibujaCirculo( g, pMed, MarcasAverias.RADIO_MARCA_RESISTENCIA, zoom );
				MarcasAverias.dibujaCirculo( g, p2, MarcasAverias.RADIO_MARCA_RESISTENCIA, zoom );
			} else if (resist==RESISTENCIA_INDEFINIDA) {  // Resistencia indefinida: círculos de color
				MarcasAverias.dibujaLinea( g, pIni, pFin, zoom );
				g.setColor( COLOR_INDEFINIDA );
				MarcasAverias.dibujaCirculo( g, p1, MarcasAverias.RADIO_MARCA_RESISTENCIA, zoom );
				MarcasAverias.dibujaCirculo( g, pMed, MarcasAverias.RADIO_MARCA_RESISTENCIA, zoom );
				MarcasAverias.dibujaCirculo( g, p2, MarcasAverias.RADIO_MARCA_RESISTENCIA, zoom );
			} else if (resist==RESISTENCIA_CERO) {  // Resistencia cero: línea sin más
				MarcasAverias.dibujaLinea( g, pIni, pFin, zoom );
			} else if (resist==RESISTENCIA_INFINITA) {  // Resistencia infinita: línea abierta
				MarcasAverias.dibujaLinea( g, pIni, pFin, zoom );
				PanelDeDibujo.dibujaSimboloEnMedio( g, pIni, pFin, zoom, "ResistenciaInfinita" );
			} else {  // Resistencia con algún valor: dibujado
				MarcasAverias.dibujaLinea( g, pIni, p1, zoom );
				MarcasAverias.dibujaLinea( g, p2, pFin, zoom );
				int numAngulos = 1;
				if (resist>1000) numAngulos = 12; else if (resist>500) numAngulos = 11; else if (resist>250) numAngulos = 10; else if (resist>125) numAngulos = 9;
				else if (resist>100) numAngulos = 8; else if (resist>50) numAngulos = 7; else if (resist>30) numAngulos = 6; else if (resist>15) numAngulos = 5;
				else if (resist>10) numAngulos = 4; else if (resist>5) numAngulos = 3; else if (resist>2) numAngulos = 2;
				dibujaResistenciaAngulos( g, zoom, p1, p2, numAngulos );
			}
		}
	}	
		// Dibujado recursivo resistencia
		private static void dibujaResistenciaAngulos( Graphics2D g, double zoom, Point p1, Point p2, int numAngulos ) {
			if (numAngulos<=1) {
				PanelDeDibujo.dibujaSimboloEntrePuntos( g, p1, p2, zoom, "Resistencia" );
			} else {
				Point pAnt = p1;
				for (int i=1; i<numAngulos; i++) {
					Point pMed = new Point( p1.x+(p2.x-p1.x)/numAngulos*i, p1.y+(p2.y-p1.y)/numAngulos*i );  // Punto intermedio
					PanelDeDibujo.dibujaSimboloEntrePuntos( g, pAnt, pMed, zoom, "Resistencia" );  // trozos
					pAnt = pMed;
					if (i==numAngulos-1) {
						PanelDeDibujo.dibujaSimboloEntrePuntos( g, pMed, p2, zoom, "Resistencia" );  // trozo final
					}
				}
			}
		}
		// Devuelve la resistencia para todos sus estados, -1 si no hay una resistencia común 
		private double getResistenciaFromTodos() {
			double rComun = -1;
			for (double r : resistencia ) {
				if (rComun == -1) rComun = r;
				else if (rComun != r) { rComun = -1; break; }
			}
			return rComun;
		}
		/*  Antigua visualización de resistencia
		private void setColorFromResistencia( Graphics2D g, double r ) {
			if (r==RESISTENCIA_INDEFINIDA)
				g.setColor( COLOR_INDEFINIDA );
			else if (r==RESISTENCIA_INFINITA)
				g.setColor( COLOR_INFINITA );
			else if (r==RESISTENCIA_CERO)
				g.setColor( COLOR_CERO );
			else {
				g.setColor( COLOR_VALOR );
				// Algo así si se quisiera graduar el color:
				// double graduacion = r / VOLTAJE_MAXIMO_COLOR;
				// g.setColor( new Color( (int)(COLOR_RMIN + (COLOR_RMAX-COLOR_RMIN)*graduacion), (int)(COLOR_GMIN + (COLOR_GMAX-COLOR_GMIN)*graduacion), (int)(COLOR_BMIN + (COLOR_BMAX-COLOR_BMIN)*graduacion)));
			}
		}
		*/
	
	private transient JPanel miPanelEdicion = null;
	private transient JLabel lInformacion = null;
	private transient JTextField tfNombre = null;
	private transient JTextField tfResistencia = null;
	@Override
	public JPanel getPanelEdicion() {
		if (miPanelEdicion == null) {
			miPanelEdicion = new JPanel();
			miPanelEdicion.setMinimumSize( new Dimension( 100, 60 ));
			JPanel linea1 = new JPanel();
			JPanel linea2 = new JPanel();
			JPanel linea3 = new JPanel();
			miPanelEdicion.setLayout( new BoxLayout( miPanelEdicion, BoxLayout.Y_AXIS ));
			miPanelEdicion.add( linea1 ); miPanelEdicion.add( linea2 ); miPanelEdicion.add( linea3 );
			linea1.add( new JLabel( "Nombre: ") );
				tfNombre = new JTextField( 10 );
				tfNombre.setText( nombre );
				linea1.add( tfNombre );
			lInformacion = new JLabel();
				linea2.add( lInformacion );
			linea3.add( new JLabel( "Cambio valor:" ) );
				tfResistencia = new JTextField( 5 );
				linea3.add( tfResistencia );
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
			tfResistencia.addFocusListener( new FocusAdapter() {
				private String valAnterior = null;
				@Override public void focusGained(FocusEvent e) {
					valAnterior = tfResistencia.getText();
				}
				@Override public void focusLost(FocusEvent e) {
					if (!tfResistencia.getText().equals( valAnterior )) {
						if (VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().isModoResistencias()) {
							String texRes = tfResistencia.getText().toUpperCase();
							if (VentanaEditorAveriaAuto.getLastVentana().isCombinacionTodasSeleccionada()) {
								try {
									double d = 0;
									if (texRes.equals("INFINITA") || texRes.equals("INF")) d = RESISTENCIA_INFINITA;
									else if (texRes.equals("IND")) d = RESISTENCIA_INDEFINIDA;
									else d = java.lang.Double.parseDouble( texRes );
									for (int i=0; i<resistencia.size(); i++) {
										resistencia.set( i, d ); resistenciaEditada.set( i, true );
									}
									VentanaEditorAveriaAuto.getLastVentana().getControlador().refrescarTablaCon( Resistencia.this );
								} catch (NumberFormatException ex) {
								}
							} else {
								int indi = VentanaEditorAveriaAuto.getLastVentana().getIndiceCombinacionSeleccionada();
								if (indi!=-1 && indi<resistencia.size()) {
									try {
										double d = 0;
										if (texRes.equals("INFINITA")) d = RESISTENCIA_INFINITA;
										else if (texRes.equals("IND")) d = RESISTENCIA_INDEFINIDA;
										else d = java.lang.Double.parseDouble( texRes );
										resistencia.set( indi, d ); resistenciaEditada.set( indi, true );
										VentanaEditorAveriaAuto.getLastVentana().getControlador().refrescarTablaCon( Resistencia.this );
									} catch (NumberFormatException ex) {
									}
								}
							}
						}
					}
				}
			});
			Component[] lC = { tfNombre, tfResistencia };
			for (Component c : lC ) c.addKeyListener( VentanaEditorAveriaAuto.getLastVentana().getControlador().escTeclado );
			for (Component c : lC ) c.addKeyListener( VentanaEditorAveriaAuto.getLastVentana().getControlador().escTecladoTextos );
		}
		lInformacion.setText( "Puntos: " + listaPuntos.size() + " - " + toDescripcion() );
		return miPanelEdicion;
	}
	
	// RESISTENCIAS
	public ArrayList<java.lang.Double> getResistencias() { return resistencia; }
	public ArrayList<Boolean> getResistenciasEditadas() { return resistenciaEditada; }
	public java.lang.Double getResistencia( int numEstadoCircuito ) { return resistencia.get(numEstadoCircuito); }
	public boolean getResistenciaEditada( int numEstadoCiruito ) { return resistenciaEditada.get(numEstadoCiruito); }
	public void initResistencias( int numEstadosCircuito ) {
		resistencia = new ArrayList<java.lang.Double>();
		resistenciaEditada = new ArrayList<Boolean>();
		for (int i=0; i<numEstadosCircuito; i++) {
			resistencia.add( RESISTENCIA_INDEFINIDA );
			resistenciaEditada.add( false );
		}
	}
	public void setResistencia( int numEstadoCircuito, double res ) {
		resistencia.set( numEstadoCircuito, res );
		resistenciaEditada.set( numEstadoCircuito, true );
	}
	public void setResistenciaEditada( int numEstadoCircuito, boolean editada ) {
		resistenciaEditada.set( numEstadoCircuito, editada );
	}
	public String toStringResistencias() {
		String ret = "{";
		for (int i=0; i<resistencia.size(); i++) {
			if (i>0) ret += ",";
			double res = resistencia.get(i);
			if (res==RESISTENCIA_CERO) ret += "0";
			else if (res==RESISTENCIA_INFINITA) ret +="oo";
			else if (res==RESISTENCIA_INDEFINIDA) ret +="-";
			else ret += res;
		}
		return ret + "}";
	}
	
	
}
