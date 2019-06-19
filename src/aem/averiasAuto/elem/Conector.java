package aem.averiasAuto.elem;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import aem.averiasAuto.TextoDeCircuito;
import aem.averiasAuto.VentanaEditorAveriaAuto;

public class Conector extends Conexion {
	private static final long serialVersionUID = 1L;
	public Conector() {
		super();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		Conector con = new Conector();
		con.listaPuntos = (ArrayList<Punto>) listaPuntos.clone();
		con.nombre = nombre;
		con.textoAsociado = textoAsociado;
		return con;
	}

	/** Convierte el objeto a String en una línea -con atributos separados por comas-
	 * @return	Versión textual del objeto
	 */
	public String aTexto() {
		String ret = nombre + ",CONECTOR";
		for (Punto p : listaPuntos) {
			ret += ("," + p.getNombre());
		}
		return ret;
	}

	/** Convierte los datos de texto del String en los atributos del conector.
	 * Si hay algún error no construye correctamente el conector y devuelve NullPointer
	 * @param s	String del conector en formato texto
	 * @param listaPuntos	Lista de todos los puntos (debe incluir los puntos nombrados en el conector)
	 * @return	Nuevo conector
	 */
	public static Conector crearConector( String s, ArrayList<Punto> listaPuntos ) throws NullPointerException {
		Conector ret = null;
		s = " " + s.trim();
		StringTokenizer st = new StringTokenizer( s, "," );
		if (st.countTokens()>=4) {
			try {
				ret = new Conector();
				ret.nombre = st.nextToken().trim();
				st.nextToken();  // token = "CONECTOR"
				while (st.hasMoreTokens()) {
					String nom = st.nextToken();
					int posi = listaPuntos.size()-1; while (posi>=0 && !listaPuntos.get(posi).equals(nom)) posi--;
					if (posi==-1) throw new NullPointerException("Error en construcción de objeto Conector desde texto (punto " + nom + " no existente)");
					ret.add( listaPuntos.get(posi) ); 
				}
			} catch (NoSuchElementException e) {
				throw new NullPointerException( "Error en construcción de objeto Conector desde texto " + s ); 
			}
		} 
		if (ret == null) throw new NullPointerException( "Error en construcción de objeto Conector desde texto " + s );
		return ret;
	}

	public TipoElemento getTipoElemento() {
		return TipoElemento.Conector;
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
	
	public String toString() { return "CX " + nombre + "(CONECTOR) -> " + listaPuntos; }
	
	public String toDescripcion() { 
		String listaPtos = "{";
		for (Punto p : listaPuntos)
			listaPtos = listaPtos + p.getNombre() + ",";
		if (listaPuntos.size()>0) listaPtos = listaPtos.substring( 0, listaPtos.length()-1 ) + "}";
		return "CX (CONECTOR) -> " + listaPtos; 
	}

	// Dos conectores son iguales si sus puntos son iguales, y además si sus nombres lo son
	// Un conector es igual a un String si el String es su nombre
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (o instanceof Conector) {
			Conector p2 = (Conector)o;
			ArrayList<Punto> puntos1 = (ArrayList<Punto>) getAL().clone();
			ArrayList<Punto> puntos2 = (ArrayList<Punto>) p2.getAL().clone();
			Collections.sort( puntos1 ); Collections.sort( puntos2 );
			if (!puntos1.equals(puntos2)) return false;  // Si los puntos no son los mismos, no son iguales
			if (nombre == null) return p2.nombre==null;
			if (nombre.equals("")) return p2.nombre.equals("");
			return (nombre.equals(p2.nombre));
		} else if (o instanceof String) {
			String n = (String)o;
			if (nombre == null) return n==null;
			return (nombre.equals(n));
		} else return false;
	}

	// Definido por si en un futuro se quiere cambiar
	/** Dibuja el objeto en el objeto graphics indicado
	 * @param g
	 */
	public void dibuja( Graphics2D g, Color col, Stroke st, double zoom ) {
		super.dibuja( g, col, st, zoom );
		// g.setColor( col ); g.setStroke( st );
		// Point pAnt = null;
		// for (Punto p : listaPuntos) {
		// 	if (pAnt != null) MarcasAverias.dibujaLinea( g, pAnt, p, zoom );
		// 	pAnt = p;
		// }
	}	
	
	private transient TextoDeCircuito textoAsociado = null;
	public void setTextoAsociado( TextoDeCircuito tdc ) { textoAsociado = tdc; }
	public TextoDeCircuito getTextoAsociado() { return textoAsociado; }
	
	private transient JPanel miPanelEdicion = null;
	private transient JLabel lInformacion = null;
	private transient JTextField tfNombre = null;
	@Override
	public JPanel getPanelEdicion() {
		if (miPanelEdicion == null) {
			miPanelEdicion = new JPanel();
			miPanelEdicion.setMinimumSize( new Dimension( 100, 60 ));
			JPanel linea1 = new JPanel();
			JPanel linea2 = new JPanel();
			miPanelEdicion.setLayout( new BoxLayout( miPanelEdicion, BoxLayout.Y_AXIS ));
			miPanelEdicion.add( linea1 );
			miPanelEdicion.add( linea2 );
			linea1.add( new JLabel( "Nombre: ") );
				tfNombre = new JTextField( 10 );
				tfNombre.setText( nombre );
				linea1.add( tfNombre );
			lInformacion = new JLabel();
				linea2.add( lInformacion );
			// Eventos
			tfNombre.addFocusListener( new FocusListener() {
				String antVal = null;
				@Override
				public void focusLost(FocusEvent e) {
					if (!tfNombre.getText().equals(antVal)) {  // Ha habido cambio de valor en el textfield
						String nuevoNombre = tfNombre.getText();
						setNombre( nuevoNombre );
						if (textoAsociado!=null) textoAsociado.setText( nuevoNombre );
					}
				}
				@Override
				public void focusGained(FocusEvent e) {
					antVal = tfNombre.getText();
				}
			});
			Component[] lC = { tfNombre };
			for (Component c : lC ) c.addKeyListener( VentanaEditorAveriaAuto.getLastVentana().getControlador().escTeclado );
			for (Component c : lC ) c.addKeyListener( VentanaEditorAveriaAuto.getLastVentana().getControlador().escTecladoTextos );
		}
		lInformacion.setText( "Puntos: " + listaPuntos.size() + " - " + toDescripcion() );
		return miPanelEdicion;
	}
}
