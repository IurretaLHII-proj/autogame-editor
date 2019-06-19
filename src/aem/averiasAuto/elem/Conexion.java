package aem.averiasAuto.elem;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import aem.averiasAuto.MarcasAverias;
import aem.averiasAuto.TextoDeCircuito;
import aem.averiasAuto.VentanaEditorAveriaAuto;

public class Conexion implements ObjetoDeGrafico, Serializable, Averiable {
	private static final long serialVersionUID = 1L;
	protected String nombre;
	protected ArrayList<Punto> listaPuntos;
	protected boolean averia = false;
	public Conexion() {
		listaPuntos = new ArrayList<Punto>();
		nombre = "";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		Conexion con = new Conexion();
		con.listaPuntos = (ArrayList<Punto>) listaPuntos.clone();
		con.nombre = nombre;
		con.textoAsociado = textoAsociado;
		con.averia = averia;
		return con;
	}

	/** Convierte el objeto a String en una línea -con atributos separados por comas-
	 * @return	Versión textual del objeto
	 */
	public String aTexto() {
		String ret = nombre + ",CONEXION";
		for (Punto p : listaPuntos) {
			ret += ("," + p.getNombre());
		}
		if (averia) ret += ",AVERIA";
		return ret;
	}

	/** Convierte los datos de texto del String en los atributos de la conexión.
	 * Si hay algún error no construye correctamente la conexión y devuelve NullPointer
	 * @param s	String de la conexión en formato texto
	 * @param listaPuntos	Lista de todos los puntos (debe incluir los puntos nombrados en la conexión)
	 * @return	Nueva conexión
	 */
	public static Conexion crearConexion( String s, ArrayList<Punto> listaPuntos ) throws NullPointerException {
		Conexion ret = null;
		s = " " + s.trim();
		StringTokenizer st = new StringTokenizer( s, "," );
		if (st.countTokens()>=4) {
			try {
				ret = new Conexion();
				ret.nombre = st.nextToken().trim();
				st.nextToken();  // token = "CONEXION"
				while (st.hasMoreTokens()) {
					String nom = st.nextToken();
					if (nom.equals("AVERIA") || nom.equals("[AVERIA]")) {
						ret.averia = true;
					} else {
						int posi = listaPuntos.size()-1; while (posi>=0 && !listaPuntos.get(posi).equals(nom)) posi--;
						if (posi==-1) throw new NullPointerException("Error en construcción de objeto Conexión desde texto (punto " + nom + " no existente)");
						ret.add( listaPuntos.get(posi) ); 
					}
				}
			} catch (NoSuchElementException e) {
				throw new NullPointerException( "Error en construcción de objeto Conexión desde texto " + s ); 
			}
		} 
		if (ret == null) throw new NullPointerException( "Error en construcción de objeto Conexion desde texto " + s );
		return ret;
	}

	public TipoElemento getTipoElemento() {
		return TipoElemento.Conexion;
	}
	
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
		if (textoAsociado!=null) {
			textoAsociado.setText( nombre );
		}
		if (tfNombre!=null) tfNombre.setText( nombre );
	}
	
	public boolean isAveriado() { return averia; }
	public void setAveriado( boolean averia ) { 
		this.averia = averia; 
		if (cbAveria!=null) cbAveria.setSelected( averia );
	}
	
	public Punto get( int index ) { return listaPuntos.get( index ); }
	public void add( Punto p ) { listaPuntos.add( p ); }
	public int size() { return listaPuntos.size(); }
	public ArrayList<Punto> getAL() { return listaPuntos; }

	@Override
	public Object getVal(String prop) {
		prop = prop.toUpperCase();
		if (prop.equals("NOMBRE")) return nombre;
		else if (prop.equals("AVERIA")) return averia;
		else return null;
	}

	@Override
	public void setVal(String prop, Object val) {
		prop = prop.toUpperCase();
		try {
			if (prop.equals("NOMBRE")) setNombre( (String) val );
			else if (prop.equals("AVERIA")) setAveriado( (Boolean) val );
		} catch (Exception e) {
			// Asignación incorrecta
			System.out.println( "Error en setVal - " + prop + " = " + val + " --> " + e.getMessage() );
		}
	}

		private static String[] props = { "NOMBRE", "AVERIA" };
		private static Class<?>[] tipos = { String.class, Boolean.class };
	
	public String[] getProps() { return props; }
	public Class<?>[] getTipos() { return tipos; }
	
	public String toString() { return "CX " + nombre + "(CONEXION) -> " + listaPuntos + (averia?" [AVERIA]":""); }
	
	public String toDescripcion() { 
		String listaPtos = "{";
		for (Punto p : listaPuntos)
			listaPtos = listaPtos + p.getNombre() + ",";
		if (listaPuntos.size()>0) listaPtos = listaPtos.substring( 0, listaPtos.length()-1 ) + "}";
		return "CX (CONEXION) -> " + listaPtos + (averia?" [AVERIA]":""); 
	}

	// Dos conexiones son iguales si sus puntos son iguales, y además si sus nombres lo son
	// Una conexión es igual a un String si el String es su nombre
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (o instanceof Conexion) {
			Conexion p2 = (Conexion)o;
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

	/** Dibuja el objeto en el objeto graphics indicado
	 * @param g
	 */
	public void dibuja( Graphics2D g, Color col, Stroke st, double zoom ) {
		g.setColor( col ); g.setStroke( st );
		Point pAnt = null;
		for (Punto p : listaPuntos) {
			if (pAnt != null) {
				MarcasAverias.dibujaLinea( g, pAnt, p, zoom );
				if (averia) {
					g.setColor( Color.red ); g.setStroke( MarcasAverias.STROKE5 );
					MarcasAverias.dibujaCruz( g, pAnt, p, 15, zoom );
				}
			}
			pAnt = p;
		}
	}	
	
	private transient TextoDeCircuito textoAsociado = null;
	public void setTextoAsociado( TextoDeCircuito tdc ) { textoAsociado = tdc; }
	public TextoDeCircuito getTextoAsociado() { return textoAsociado; }
	
	private transient JPanel miPanelEdicion = null;
	private transient JLabel lInformacion = null;
	private transient JTextField tfNombre = null;
	private transient JCheckBox cbAveria = null;
	@Override
	public JPanel getPanelEdicion() {
		if (miPanelEdicion == null) {
			miPanelEdicion = new JPanel();
			miPanelEdicion.setMinimumSize( new Dimension( 100, 60 ));
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
			lInformacion = new JLabel();
				linea2.add( lInformacion );
			cbAveria = new JCheckBox( "¿Avería?" );
				cbAveria.setSelected( averia );
				linea3.add( cbAveria );
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
			cbAveria.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setAveriado( cbAveria.isSelected() );
					VentanaEditorAveriaAuto.getLastVentana().getControlador().redibujaCircuito();
				}
			});
			Component[] lC = { tfNombre, cbAveria };
			for (Component c : lC ) c.addKeyListener( VentanaEditorAveriaAuto.getLastVentana().getControlador().escTeclado );
			for (Component c : lC ) c.addKeyListener( VentanaEditorAveriaAuto.getLastVentana().getControlador().escTecladoTextos );
		}
		lInformacion.setText( "Puntos: " + listaPuntos.size() + " - " + toDescripcion() );
		return miPanelEdicion;
	}
}
