package aem.averiasAuto;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JPanel;

import aem.averiasAuto.elem.*;

/** Permite crear un JTextField asociado a un objeto de circuito, de modo que puede saberse
 * el objeto al que califica el nombre introducido en el cuadro de texto.
 * @author Andoni Eguíluz Morán
 * Facultad de Ingeniería - Universidad de Deusto
 */
@SuppressWarnings("serial")
public class TextoDeCircuito extends javax.swing.JTextField implements Accion {
	private ObjetoDeGrafico objetoAsociado;
	private static final int LONG_MINIMA_TEXTO = 2;
	private static final int LONG_MAXIMA_TEXTO = 8;
	private int posXAbsoluta = Integer.MAX_VALUE;
	private int posYAbsoluta = Integer.MAX_VALUE;
	
	/** Construye un nuevo cuadro de texto, indicando la anchura en columnas y el objeto de circuito al que se asocia
	 * @param columnas	Número aproximado de columnas (caracteres) que caben en la caja
	 * @param objetoAsociado	Objeto al que se asocia el cuadro de texto (NO DEBE SER null!!!)
	 */
	public TextoDeCircuito( int columnas, ObjetoDeGrafico objetoAsociado ) {
		super( columnas );
		this.objetoAsociado = objetoAsociado;
		objetoAsociado.setTextoAsociado( this );
		addFocusListener( escTexto );
		addMouseListener( escRaton );
		addMouseMotionListener( escMovtoRaton );
		addKeyListener( ControladorVentanaEAA.getLast().escTeclado );
	}
	
	@Override
	public Object clone() {
		TextoDeCircuito tcir = new TextoDeCircuito( getColumns(), objetoAsociado );
		tcir.setLocation( getLocation() );
		tcir.setLocationAbsoluta( getLocationAbsoluta().x, getLocationAbsoluta().y );
		return tcir;
	}
	
	/** Convierte el objeto a String en una línea -con atributos separados por comas-
	 * @return	Versión textual del objeto
	 */
	public String aTexto() {
		if (posXAbsoluta==Integer.MAX_VALUE)
			return getColumns() + "," + objetoAsociado.getClass().getSimpleName() + "," + objetoAsociado.getNombre() + 
					"," + getLocation().x + "," + getLocation().y;
		else
			return getColumns() + "," + objetoAsociado.getClass().getSimpleName() + "," + objetoAsociado.getNombre() + 
				"," + posXAbsoluta + "," + posYAbsoluta;
	}
	
	/** Convierte los datos de texto del String en los atributos del texto de circuito asociado.
	 * Si hay algún error no construye correctamente el texto y devuelve NullPointer
	 * @param s	String del texto de circuito en formato texto
	 * @param listaPuntos	Lista de todos los puntos (debe incluir los puntos nombrados en el componente)
	 * @param listaConexiones	Lista de todas las conexiones (debe incluir los nombrados en el componente)
	 * @param listaConectores	Lista de todos los conectores (debe incluir los nombrados en el componente)
	 * @param listaComponentes	Lista de todos los compos (debe incluir los nombrados en el componente)
	 * @return	Nuevo texto de circuito
	 */
	public static TextoDeCircuito crearTextoDeCircuito( String s, ArrayList<Punto> listaPuntos,
			ArrayList<Conexion> listaConexiones, ArrayList<Conector> listaConectores, ArrayList<Componente> listaComponentes,
			double zoomActual ) throws NullPointerException {
		TextoDeCircuito ret = null;
		s = s.trim();
		StringTokenizer st = new StringTokenizer( s, "," );
		if (st.countTokens()==3 || st.countTokens()==5) {   // Versiones anteriores (3) no tenían las coordenadas (5)
			try {
				String tok = st.nextToken();
				int numCols = Integer.parseInt( tok );
				ObjetoDeGrafico obj = null;
				tok = st.nextToken();
				String nombre = st.nextToken();
				int x = 0; int y = 0;
				if (tok.equals("Punto")) {
					int posi = listaPuntos.size()-1; while (posi>=0 && !listaPuntos.get(posi).equals(nombre)) posi--;
					if (posi==-1) throw new NullPointerException("Error en construcción de objeto Texto desde texto (punto " + nombre + " no existente) - puntos " + listaPuntos);
					obj = listaPuntos.get(posi);
					x = listaPuntos.get(posi).x; y = listaPuntos.get(posi).y;
				} else if (tok.equals("Componente")) {
					int posi = listaComponentes.size()-1; while (posi>=0 && !listaComponentes.get(posi).equals(nombre)) posi--;
					if (posi==-1) throw new NullPointerException("Error en construcción de objeto Texto desde texto (componente " + nombre + " no existente)");
					obj = listaComponentes.get(posi);
					x = listaComponentes.get(posi).getAL().get(0).x; y = listaComponentes.get(posi).getAL().get(0).y;
				} else if (tok.equals("Conexion")) {
					int posi = listaConexiones.size()-1; while (posi>=0 && !listaConexiones.get(posi).equals(nombre)) posi--;
					if (posi==-1) {
						posi = listaConectores.size()-1; while (posi>=0 && !listaConectores.get(posi).equals(nombre)) posi--;
						if (posi==-1) throw new NullPointerException("Error en construcción de objeto Texto desde texto (conexión/conector " + nombre + " no existente)");
						obj = listaConectores.get(posi);
						x = listaConectores.get(posi).getAL().get(0).x; y = listaConectores.get(posi).getAL().get(0).y;
					} else {
						obj = listaConexiones.get(posi);
						x = listaConexiones.get(posi).getAL().get(0).x; y = listaConexiones.get(posi).getAL().get(0).y;
					}
				} 
				x = x + MarcasAverias.DIST_X_TEXTO; y = y + MarcasAverias.DIST_Y_TEXTO;  // Posiciones por defecto de los textos 
				if (st.hasMoreTokens()) tok = st.nextToken();
				if (st.hasMoreTokens()) {  // Si hay 5 hay coordenadas, coger las posiciones grabadas
					x = Integer.parseInt( tok );
					tok = st.nextToken();
					y = Integer.parseInt( tok );
				}
				ret = new TextoDeCircuito(numCols, obj );
				ret.setLocation( x, y );
				ret.setSize( MarcasAverias.ANCH_TEXTO_INICIAL, MarcasAverias.ALT_TEXTO );
				ret.setLocationConZoom( zoomActual );
				ret.setText( nombre );
			} catch (NoSuchElementException | NumberFormatException e) {
				throw new NullPointerException( "Error en construcción de objeto Texto desde texto " + s ); 
			}
		} 
		if (ret == null) throw new NullPointerException( "Error en construcción de objeto Texto desde texto " + s );
		return ret;
	}

	public static void reloadPanel( JPanel panelEdi ) {
		// Sin función - de momento
	}
	
	public void reloadListener() {
		FocusListener[] fls = getFocusListeners();
		for (FocusListener fl : fls) removeFocusListener(fl);
		addFocusListener( escTexto );
		MouseMotionListener[] mmls = getMouseMotionListeners();
		for (MouseMotionListener mml : mmls) removeMouseMotionListener(mml);
		addMouseMotionListener( escMovtoRaton );
		MouseListener[] mls = getMouseListeners();
		for (MouseListener ml : mls) removeMouseListener(ml);
		addMouseListener( escRaton );
	}
	public ObjetoDeGrafico getObjetoAsociado() { return objetoAsociado; }
	
	private volatile static FocusListener escTexto = new FocusListener() {
		String nombreAnt = "";
		@Override
		public void focusLost(FocusEvent e) {
			TextoDeCircuito tdc = (TextoDeCircuito) (e.getSource());
			String nombreNuevo = tdc.getText();
			if (!nombreNuevo.equals(nombreAnt)) {  // Hay cambio
				 tdc.getObjetoAsociado().setVal( "nombre", nombreNuevo );
				 String nombrePuesto = (String) tdc.getObjetoAsociado().getVal("nombre");  // Si el nombre es repetido se obliga al cambio interactivo
				 if (nombrePuesto!=null && !nombrePuesto.equals(nombreNuevo)) tdc.setText( nombrePuesto );
				 VentanaEditorAveriaAuto.getLastVentana().getControlador().refrescaObjetoEnTabla( tdc.getObjetoAsociado() );
			}
			// Cambio longitud adaptándose al texto
			tdc.recalcAnchura();
			// if (panelEdicion != null) {
			// 	panelEdicion.requestFocus();
			// }
		}
		@Override
		public void focusGained(FocusEvent e) {
			nombreAnt = ((TextoDeCircuito) (e.getSource())).getText();
		}
	};
	
	private void recalcAnchura() {
		int longitud = getText().length();
		if (longitud < LONG_MINIMA_TEXTO) longitud = LONG_MINIMA_TEXTO;
		else if (longitud > LONG_MAXIMA_TEXTO) longitud = LONG_MAXIMA_TEXTO;
		setSize( MarcasAverias.ANCH_TEXTO_MINIMA + MarcasAverias.ANCH_TEXTO_POR_CAR * longitud, MarcasAverias.ALT_TEXTO );
	}
	
	private volatile static Point puntoInicio = null;
	private volatile static MouseListener escRaton = new MouseAdapter() {
		@Override
		public void mouseReleased(MouseEvent e) {
			if (puntoInicio!=null) {
				Point puntoFin = e.getPoint();
				TextoDeCircuito tc = (TextoDeCircuito) e.getSource();
				Point ref = tc.getLocation();
				ref.translate( puntoFin.x - puntoInicio.x, puntoFin.y - puntoInicio.y );
				tc.setLocation( ref );
				tc.setLocationAbsoluta( ref.x, ref.y, VentanaEditorAveriaAuto.getLastVentana().getPanelDibujo().getZoom() );
				Acciones.getInstance().hacer( "Mover texto" );
			}
			puntoInicio = null;
			VentanaEditorAveriaAuto.getLastVentana().getPanelDibujo().repaint();  // Quita marcas del drag
		}
		@Override
		public void mousePressed(MouseEvent e) {
			puntoInicio = e.getPoint();
		}
	};

	private volatile static MouseMotionListener escMovtoRaton = new MouseMotionAdapter() {
		@Override
		public void mouseDragged(MouseEvent e) {  // Dibuja marcas del drag
			if (puntoInicio!=null) {
				TextoDeCircuito tc = (TextoDeCircuito)e.getSource();
				Graphics2D g2 = (Graphics2D) VentanaEditorAveriaAuto.getLastVentana().getPanelDibujo().getGraphics();
				g2.drawRect( e.getPoint().x-(int)puntoInicio.getX()+tc.getX(), e.getPoint().y-(int)puntoInicio.getY()+tc.getY(), tc.getWidth(), tc.getHeight() );
			}
		}
	};
		
	public String toString() {
		return "{T (" + getLocation().x + "," + getLocation().y +") " + objetoAsociado + "}";
	}

	@Override
	public void setText(String t) {
		super.setText(t);
		recalcAnchura();
	}

	public Point getLocationAbsoluta() {
		return new Point( posXAbsoluta, posYAbsoluta );
	}

	public void setLocationAbsoluta(int x, int y) {
		posXAbsoluta = x;
		posYAbsoluta = y;
	}

	/** Pone la posición absoluta con el zoom correspondiente
	 * @param x	X visual en la que se quiere poner
	 * @param y Y visual en la que se quiere poner
	 * @param zoom	Zoom actual de la pantalla
	 */
	public void setLocationAbsoluta(int x, int y, double zoom ) {
		posXAbsoluta = (int) Math.round(x/zoom);
		posYAbsoluta = (int) Math.round(y/zoom);
	}

	/** Pone la posición visual sin cambiar la lógica
	 * @param x
	 * @param y
	 */
	public void setLocationVisual(int x, int y) {
		super.setLocation(x, y);
		if (posXAbsoluta==Integer.MAX_VALUE) setLocationAbsoluta( x, y );
	}

	/** Pone la posición visual y la lógica
	 * @param x	Posición visual nueva (x)
	 * @param y	Posición visual nueva (y)
	 * @param zoom	Zoom actual
	 */
	public void setLocationConZoom(int x, int y, double zoom ) {
		super.setLocation(x, y);
		if (posXAbsoluta==Integer.MAX_VALUE)
			setLocationAbsoluta( (int)Math.round( x / zoom ), (int)Math.round( y / zoom ) );
	}

	/** Actualiza la posición visual manteniendo la lógica
	 * @param zoom	Zoom actual
	 */
	public void setLocationConZoom( double zoom ) {
		if (posXAbsoluta==Integer.MAX_VALUE)
			setLocationAbsoluta( getLocation().x, getLocation().y );
		int posX = (int)Math.round(posXAbsoluta * zoom);
		int posY = (int)Math.round(posYAbsoluta * zoom);
		super.setLocation( posX, posY );
	}

	@Override
	public void setLocation(int x, int y) {
		super.setLocation(x, y);
		if (posXAbsoluta == Integer.MAX_VALUE)
			setLocationAbsoluta(x, y);
	}

	@Override
	public void setLocation(Point p) {
		super.setLocation(p);
		if (posXAbsoluta == Integer.MAX_VALUE)
			setLocationAbsoluta(p.x, p.y);
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		if (posXAbsoluta == Integer.MAX_VALUE)
			setLocationAbsoluta(x, y);
	}

	/** Pone bounds con un zoom en pantalla existente
	 */
	public void setBounds(int x, int y, int width, int height, double zoom) {
		super.setBounds(x, y, width, height);
		setLocationAbsoluta( (int)Math.round(x/zoom), (int)Math.round(y/zoom) ); 
	}

	@Override
	public void setBounds(Rectangle r) {
		super.setBounds(r);
		if (posXAbsoluta == Integer.MAX_VALUE)
			setLocationAbsoluta(r.x, r.y);
	}

	
	
}
