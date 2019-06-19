package aem.averiasAuto.elem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import aem.averiasAuto.*;
import static aem.averiasAuto.PanelDeDibujo.*;

public class Componente implements ObjetoDeGrafico, Serializable, Averiable {
	private static final long serialVersionUID = 1L;
	protected String nombre;
	protected String tipo;
	protected String config1, config2, config3;    // Configuraciones particulares del componente
	protected int numTipo;
	private ArrayList<Punto> listaPuntos;
	protected boolean averia = false;
	protected boolean seleccionable = true;  // v0.25 - true para los componentes seleccionables (lo habitual), false para los no seleccionables en reproducción
	private Componente( int tipoComp ) {
		listaPuntos = new ArrayList<Punto>();
		nombre = nombresComponentes[tipoComp];
		tipo = nombresComponentes[tipoComp];
		numTipo = tipoComp;
		config1 = ""; config2 = ""; config3 = ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		Componente com = new Componente( numTipo );
		com.listaPuntos = (ArrayList<Punto>) listaPuntos.clone();
		com.nombre = nombre;
		com.tipo = tipo;
		com.numTipo = numTipo;
		com.textoAsociado = textoAsociado;
		com.config1 = config1;
		com.config2 = config2;
		com.config3 = config3;
		com.averia = averia;
		return com;
	}
	
	/** Convierte el objeto a String en una línea -con atributos separados por comas-
	 * @return	Versión textual del objeto
	 */
	public String aTexto() {
		String ret = nombre + "," + tipo + ",(" + config1 + ";" + config2 + ";" + config3 + ")," + numTipo + "," + seleccionable;  // v0.25 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		for (Punto p : listaPuntos) {
			ret += ("," + p.getNombre()); //$NON-NLS-1$
		}
		// Activación según interrupción
		if (isActivable()) {
			ArrayList<CombinacionInterrupcion> combs = ControladorVentanaEAA.getLast().getMarcasAverias().getCombsInterrupcion();
			if (combs.size()>0) {
				for (int i=0; i<combs.size(); i++) {
					ret = ret + "," + getActivacion(i); //$NON-NLS-1$
				}
			}
		}
		if (averia) ret += ",AVERIA"; //$NON-NLS-1$
		return ret;
	}
	
	/** Devuelve la información de activación de un componente activable (lámpara, bocina, motor)
	 * @param numCombInterrupcion	Combinación de interrupción en la que quiere saberse su activación
	 * @return	"1" si está activo, "I1000" si está intermitente, "0" en cualquier otro caso
	 */
	public String getActivacion( int numCombInterrupcion ) {
		// Los componentes activables tienen todos solo 2 puntos
		Punto p1 = listaPuntos.get(0);
		Punto p2 = listaPuntos.get(1);
		String activacion = "0"; //$NON-NLS-1$
		double volt1 = p1.getVoltaje( numCombInterrupcion );
		double volt2 = p2.getVoltaje( numCombInterrupcion );
		if (volt1<volt2) { double temp = volt1; volt1 = volt2; volt2 = temp; }
		if (volt1==Punto.VOLTAJE_MAXIMO && volt2==Punto.VOLTAJE_MINIMO) {
			activacion = "1"; //$NON-NLS-1$
		} else if (volt1==Punto.VOLTAJE_MINIMO && volt2==Punto.VOLTAJE_INTERMITENTE1000) {
			activacion = "I1000"; //$NON-NLS-1$
		}
		return activacion;
	}
	
	/** Informa de si un componente es activable o no
	 * @return	true si es lámpara, bocina o motor. false en caso contrario
	 */
	public boolean isActivable() {
		return Arrays.asList(componentesActivables).contains( tipo );
	}

	public void setSeleccionable( boolean seleccionable ) {
		this.seleccionable = seleccionable;
	}
	public boolean isSeleccionable() {
		return seleccionable;
	}

	public String getConfig1() { return config1; }
	public String getConfig2() { return config2; }
	public String getConfig3() { return config3; }
	
	/** Convierte los datos de texto del String en los atributos del componente.
	 * Si hay algún error no construye correctamente el componente y devuelve NullPointer
	 * @param s	String del componente en formato texto
	 * @param listaPuntos	Lista de todos los puntos (debe incluir los puntos nombrados en el componente)
	 * @return	Nuevo componente
	 */
	public static Componente crearComponente( String s, ArrayList<Punto> listaPuntos ) throws NullPointerException {
		Componente ret = null;
		s = s.trim();
		StringTokenizer st = new StringTokenizer( s, "," ); //$NON-NLS-1$
		if (st.countTokens()>=3) {
			try {
				String conf1 = ""; String conf2 = ""; String conf3 = "";  // Configuraciones por defecto //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				String nomb = st.nextToken(); // nombre
				String tipo = st.nextToken(); // tipo (debe coincidir con el array estático)
				int numTipo = Arrays.asList( nombresComponentes ).indexOf( tipo );
				String tok = st.nextToken();
				if (tok.startsWith("(") && tok.endsWith(")")) { // Versión 0.04 o superior - tiene configuraciones de componente //$NON-NLS-1$ //$NON-NLS-2$
					tok = tok.substring( 1, tok.length()-1 );  // Quitar paréntesis
					StringTokenizer st2 = new StringTokenizer( tok, ";" ); // Separar las configuraciones //$NON-NLS-1$
					if (st2.hasMoreTokens()) conf1 = st2.nextToken();
					if (st2.hasMoreTokens()) conf2 = st2.nextToken();
					if (st2.hasMoreTokens()) conf3 = st2.nextToken();
					tok = st.nextToken();
				}
				if (numTipo==-1) // Si el string de tipo no es correcto, se usa en su lugar el código de tipo
					numTipo = Integer.parseInt( tok );
				ret = new Componente( numTipo );
				ret.nombre = nomb;
				ret.setConfig1( conf1 );
				ret.setConfig2( conf2 );
				ret.setConfig3( conf3 );
				while (st.hasMoreTokens()) {
					String nom = st.nextToken();
					if (nom.equals("true") || nom.equals("false")) { // Versión 0.25 o superior //$NON-NLS-1$ //$NON-NLS-2$
						ret.setSeleccionable( Boolean.parseBoolean( nom ) );
					} else if (nom.equals("AVERIA") || nom.equals("[AVERIA]")) { //$NON-NLS-1$ //$NON-NLS-2$
						ret.averia = true;
					} else if (nom.equals("1") || nom.equals("0") || nom.equals("I") || nom.equals("I1000")) {  // Nada (estado de activación del componente, se calcula en línea, no hay que cargarlo //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					} else {
						int posi = listaPuntos.size()-1; while (posi>=0 && !listaPuntos.get(posi).equals(nom)) posi--;
						if (posi==-1) throw new NullPointerException("Error en construcción de objeto Componente desde texto (punto " + nom + " no existente)"); //$NON-NLS-1$ //$NON-NLS-2$
						ret.listaPuntos.add( listaPuntos.get(posi) ); 
					}
				}
			} catch (NoSuchElementException | NumberFormatException e) {
				throw new NullPointerException( "Error en construcción de objeto Componente desde texto " + s );  //$NON-NLS-1$
			}
		} 
		if (ret == null) throw new NullPointerException( "Error en construcción de objeto Componente desde texto " + s ); //$NON-NLS-1$
		return ret;
	}
	
	public static String[] nombresComponentes = { "Fusible", "Batería", "Masa", "Lámpara", "Pot.alt.luces", "Pot3", "Relé4", "Relé5", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"Int2", "Int3", "Int4", "Int5", "Int6", "Motor", "Bocina", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		"Elec" }; //$NON-NLS-1$
	private static int[] numPuntosDeComponente = { 2, 2, 2, 2, 5, 3, 4, 5, 
		2, 3, 4, 5, 6, 2, 2,
		2 };
	private static String[] sufijosPuntos = { "e s", "12 G", "G1 G2", "e s", "1 2 3 4 5", "e s res", "eC sC eI sI1", "eC sC eI sI1 sI2",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		"e s1", "e s1 s2", "e s1 s2 s3", "e s1 s2 s3 s4", "e s1 s2 s3 s4 s5", "e s", "e s", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		"p" }; //$NON-NLS-1$
	private static String[] componentesActivables = { "Lámpara", "Motor", "Bocina" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
	public static Componente crearFusible() { return new Componente( 0 ); }
	public static Componente crearBateria() { return new Componente( 1 ); }
	public static Componente crearLampara() { return new Componente( 2 ); }
	public static Componente crearPotAltLuces() { return new Componente( 3 ); }
	public static Componente crearRele() { return new Componente( 4 ); }
	/** Crea y devuelve un nuevo componente de acuerdo a su tipo
	 * @param nomComponente	Tipo del componente (valor del array {@link #nombresComponentes})
	 * @return	Componente creado, null si el tipo es incorrecto
	 */
	public static Componente crearComponente( String nomComponente ) {
		for (int i=0; i<nombresComponentes.length; i++) {
			if (nombresComponentes[i].equalsIgnoreCase( nomComponente ))
				return new Componente( i );
		}
		return null;
	}
	
	public boolean isInterruptor() {
		return tipo.startsWith("Int"); //$NON-NLS-1$
	}
	
	public boolean isElectronica() {
		return tipo.equals("Elec"); //$NON-NLS-1$
	}
	
	/** Devuelve el sufijo por defecto para el nombre del punto iésimo del componente
	 * @param i	índice de 0 a n-1 del punto del componente
	 * @return	Sufijo por defecto, "" si no hay ninguno
	 */
	public String getSufijoPunto( int i ) {
		String sufs = sufijosPuntos[numTipo];
		StringTokenizer st = new StringTokenizer( sufs, " " ); //$NON-NLS-1$
		String suf = ""; int j=0; //$NON-NLS-1$
		while (st.hasMoreTokens() && j<i) { st.nextToken(); j++; }
		if (st.hasMoreTokens()) suf = st.nextToken();
		return suf;
	}
	
	public TipoElemento getTipoElemento() {
		return TipoElemento.Componente;
	}
	
	public String getNombre() { return nombre; }
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
		
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public int getNumTipo() { return numTipo; }
	public void setNumTipo( int numTipo ) { this.numTipo = numTipo; }
	
	public Punto get( int index ) { return listaPuntos.get( index ); }
	public void add( Punto p ) { listaPuntos.add( p ); }
	public int size() { return listaPuntos.size(); }
	public ArrayList<Punto> getAL() { return listaPuntos; }
	
	/** Devuelve el número de puntos de conexión que tiene este componente
	 * @return	Número de puntos (>= 2)
	 */
	public int getNumPuntos() { return numPuntosDeComponente[numTipo]; }
	
	/** Devuelve el número de puntos de conexión que tiene el tipo de componente indicado
	 * @return	Número de puntos (>= 2). -1 si el tipo de componente indicado no existe
	 */
	public static int getNumPuntos( String tipoComp ) { 
		for (int i=0; i<nombresComponentes.length; i++)
			if (nombresComponentes[i].equalsIgnoreCase( tipoComp ))
				return numPuntosDeComponente[i];
		return -1;
	}

	@Override
	public Object getVal(String prop) {
		prop = prop.toUpperCase();
		if (prop.equals("NOMBRE")) return nombre; //$NON-NLS-1$
		else if (prop.equals("TIPO")) return tipo; //$NON-NLS-1$
		else if (prop.equals("AVERIA")) return averia; //$NON-NLS-1$
		else return null;
	}

	@Override
	public void setVal(String prop, Object val) {
		prop = prop.toUpperCase();
		try {
			if (prop.equals("NOMBRE")) setNombre( (String) val ); //$NON-NLS-1$
			else if (prop.equals("TIPO")) tipo = (String) val; //$NON-NLS-1$
			else if (prop.equals("AVERIA")) setAveriado( (Boolean)val ); //$NON-NLS-1$
		} catch (Exception e) {
			// Asignación incorrecta
			System.out.println( "Error en setVal - " + prop + " = " + val + " --> " + e.getMessage() ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

		private static String[] props = { "NOMBRE", "TIPO", "AVERIA" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		private static Class<?>[] tipos = { String.class, String.class, Boolean.class };
	
	public String[] getProps() { return props; }
	public Class<?>[] getTipos() { return tipos; }
	
	public String toString() { return "CP " + nombre + "(" + tipo + ") -> " + listaPuntos + (averia?" [AVERIA]":""); } //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	public String toDescripcion() { 
		String listaPtos = "{"; //$NON-NLS-1$
		for (Punto p : listaPuntos)
			listaPtos = listaPtos + p.getNombre() + ","; //$NON-NLS-1$
		if (listaPuntos.size()>0) listaPtos = listaPtos.substring( 0, listaPtos.length()-1 ) + "}"; //$NON-NLS-1$
		return "CP " + (tipo.equals("")?"":("("+tipo+")")) + " -> " + listaPtos + (averia?" [AVERIA]":"");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	}
	
	// Dos componentes son iguales si sus nombres lo son. Un componente es igual a un String si el String es su nombre
	@Override
	public boolean equals(Object o) {
		if (o instanceof Componente) {
			Componente p2 = (Componente)o;
			if (nombre == null) return p2.nombre==null;
			return (nombre.equals(p2.nombre));
		} else if (o instanceof String) {
			String n = (String)o;
			if (nombre == null) return n==null;
			return (nombre.equals(n));
		} else return false;
	}

	
	// TODO  Dibujado diferenciado de:
//	Fusible
//	Batería
//	Masa
//	Lámpara
//	Pot.alt.luces
//	Pot
//	Relé
//	Int
//	Motor
//	Bocina 
//	Elec

	
		private static Stroke STROKE_INTERRUPTOR = new BasicStroke(2);
		private static Stroke STROKE_RELE_Y_POT = new BasicStroke(1);
		private static Color COLOR_INTERRUPTOR = Color.blue;
	/** Dibuja el objeto en el objeto graphics indicado
	 * @param g
	 */
	public void dibuja( Graphics2D g, Color col, Stroke st, double zoom ) {
		// Especial activables
		if (isActivable()) {
			boolean tamanyo = true;  // Preparado para círculo máximo, podría ser mínimo con false
			if (VentanaEditorAveriaAuto.getLastVentana().isCombinacionTodasSeleccionada()) {   // Mirar si hay un voltaje único para todos los estados
				boolean activ = true;
				for (int estado=0; estado<ControladorVentanaEAA.getLast().getMarcasAverias().getCombsInterrupcion().size(); estado++) {
					if (getActivacion(estado).equals("0")) activ = false; //$NON-NLS-1$
				}
				if (activ) {
					g.setColor( new Color(255, 255, 0, 255) );
					MarcasAverias.dibujaCirculo( g, getPuntoEnMedio( listaPuntos.toArray(new Punto[0]) ), getAnchoAlto( tamanyo, listaPuntos.toArray(new Punto[0]) )/2, zoom );
				}
			} else {
				int estado = VentanaEditorAveriaAuto.getLastVentana().getIndiceCombinacionSeleccionada();
				if (estado!=-1) {
					String activ = getActivacion( estado );
					if (activ.startsWith( "I" ) || activ.equals("1")) { //$NON-NLS-1$ //$NON-NLS-2$
						g.setColor( new Color(255, 255, 0, 155) );
						// System.out.println( (int)Math.round(getAnchoAlto( tamanyo,(listaPuntos.toArray(new Punto[0]) ))/2.0*zoom) );
						// System.out.println( getAnchoAlto( tamanyo,(listaPuntos.toArray(new Punto[0]) )) + " -- zoom " + zoom  );
						MarcasAverias.dibujaCirculoRelleno( g, getPuntoEnMedio( listaPuntos.toArray(new Punto[0]) ), (int)Math.round(getAnchoAlto( tamanyo, (listaPuntos.toArray(new Punto[0]) ))/2.0*zoom), zoom );
					}
				}
			}
		}
		g.setColor( col ); g.setStroke( st );
		Point pAnt = null;
		boolean dibujadoEstandar = true;
		if (tipo.equals("Relé5")) { //$NON-NLS-1$
			dibujadoEstandar = false;
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
			Point pCentroRele = new Point((listaPuntos.get(0).x+listaPuntos.get(1).x)/2,(listaPuntos.get(0).y+listaPuntos.get(1).y)/2);
			int xMin = 10000; int xMax = -10000; int yMin = 10000; int yMax = -10000;
			for (int i=2; i<5; i++) {
				Point p = listaPuntos.get(i);
				if (p.x < xMin) xMin = p.x; if (p.x > xMax) xMax = p.x; if (p.y < yMin) yMin = p.y; if (p.y > yMax) yMax = p.y;
			}
			// Point pCentro = MarcasAverias.aplicaZoom(new Point((xMin+xMax)/2,(yMin+yMax)/2), zoom);
			Point pCentro = new Point((xMin+xMax)/2,(yMin+yMax)/2);
			g.setStroke( STROKE_RELE_Y_POT );
			MarcasAverias.dibujaLinea( g, pCentroRele, pCentro, zoom );
			g.setStroke( st );
			dibujaSimboloEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom, "Rele" ); //$NON-NLS-1$
			MarcasAverias.dibujaLinea( g, listaPuntos.get(2), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(3), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(4), pCentro, zoom );
			dibujaCirculitoEnMedio( g, pCentro, pCentro, zoom, MarcasAverias.ANCHO_MEDIO_MARCA_COMP*2, true );
			dibujaSimboloEnMedio( g, pCentro, pCentro, zoom, "Int2" ); //$NON-NLS-1$
		} else if (tipo.equals("Relé4")) { //$NON-NLS-1$
			dibujadoEstandar = false;
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
			Point pCentroRele = new Point((listaPuntos.get(0).x+listaPuntos.get(1).x)/2,(listaPuntos.get(0).y+listaPuntos.get(1).y)/2);
			Point pCentro = new Point((listaPuntos.get(2).x+listaPuntos.get(3).x)/2,(listaPuntos.get(2).y+listaPuntos.get(3).y)/2);
			g.setStroke( STROKE_RELE_Y_POT );
			MarcasAverias.dibujaLinea( g, pCentroRele, pCentro, zoom );
			g.setStroke( st );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(2), listaPuntos.get(3), zoom );
			dibujaSimboloEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom, "Rele" ); //$NON-NLS-1$
			dibujaSimboloEnMedio( g, listaPuntos.get(2), listaPuntos.get(3), zoom, "Int2" ); //$NON-NLS-1$
		// TODO } else if (tipo.equals("Pot.alt.luces")) { //$NON-NLS-1$
		} else if (tipo.equals("Pot3")) { //$NON-NLS-1$
			dibujadoEstandar = false;
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
			Point pCentroResistPot = new Point((listaPuntos.get(0).x+listaPuntos.get(1).x)/2,(listaPuntos.get(0).y+listaPuntos.get(1).y)/2);
			g.setStroke( STROKE_RELE_Y_POT );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(2), pCentroResistPot, zoom );
			g.setStroke( st );
			dibujaSimboloEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom, "Potenciometro2" ); //$NON-NLS-1$
		} else if (tipo.equals("Fusible")) { //$NON-NLS-1$
			dibujadoEstandar = false;
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
				dibujaSimboloEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom, "Fusible" ); //$NON-NLS-1$
		} else if (tipo.equals("Batería")) { //$NON-NLS-1$
			dibujadoEstandar = false;
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
				dibujaSimboloEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom, "Bateria" ); //$NON-NLS-1$
		} else if (tipo.equals("Masa")) { //$NON-NLS-1$
			dibujadoEstandar = false;
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
				dibujaSimboloEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom, "Masa" ); //$NON-NLS-1$
		} else if (tipo.equals("Bocina")) { //$NON-NLS-1$
			dibujadoEstandar = false;
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
				dibujaSimboloEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom, "Bocina" ); //$NON-NLS-1$
		} else if (tipo.equals("Motor")) { //$NON-NLS-1$
			dibujadoEstandar = false;
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
				dibujaSimboloEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom, "Motor" ); //$NON-NLS-1$
		} else if (tipo.equals("Lámpara")) { //$NON-NLS-1$
			dibujadoEstandar = false;
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
				dibujaSimboloEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom, "Lampara" ); //$NON-NLS-1$
		} else if (tipo.equals("Int2")) { //$NON-NLS-1$
			dibujadoEstandar = false;
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
			dibujaSimboloEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom, "Int2" ); //$NON-NLS-1$
			// dibujaCirculitoEnMedio( g, listaPuntos.get(0), listaPuntos.get(1), zoom );
		} else if (tipo.equals("Int3")) { //$NON-NLS-1$
			dibujadoEstandar = false;
		} else if (tipo.equals("Int4")) { //$NON-NLS-1$
			dibujadoEstandar = false;
		} else if (tipo.equals("Int5")) { //$NON-NLS-1$
			dibujadoEstandar = false;
		} else if (tipo.equals("Int6")) { //$NON-NLS-1$
			dibujadoEstandar = false;
		} else if (tipo.equals("Elec")) { //$NON-NLS-1$
			dibujadoEstandar = false;
		}
		// Especial interruptor seleccionado en modo interruptor
		if (isInterruptor() && ControladorVentanaEAA.getLast().getEstado()==ControladorVentanaEAA.Estado.INTERRUPTOR &&
			VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().getElementoSeleccionado()==this) {
			ArrayList<ArrayList<Integer>> conexionesInt = getConexionesInterruptor();
			for (ArrayList<Integer> con : conexionesInt) {
				int pto1 = con.get(0); int pto2 = con.get(1);
				g.setColor( Color.red ); g.setStroke( MarcasAverias.STROKE5 );
				MarcasAverias.dibujaLinea( g, listaPuntos.get(pto1), listaPuntos.get(pto2), zoom );
				// System.out.println( listaPuntos.get(pto1) + " -> " + listaPuntos.get(pto2) );
				g.setColor( col ); g.setStroke( st );
			}
		}
		// Especial interruptores en modo edición de voltajes
		if (tipo.startsWith("Int") && !VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().isModoEdicion()) { //$NON-NLS-1$
			CombinacionInterrupcion ci = VentanaEditorAveriaAuto.getLastVentana().getCombinacionSeleccionada();
			g.setColor( COLOR_INTERRUPTOR ); g.setStroke( STROKE_INTERRUPTOR );
			if (ci!=null && !ci.isCombinacionTodas()) {
				for (int i=0; i<ci.getLInterruptores().size(); i++) {
					Componente c = ci.getLInterruptores().get( i );
					if (c.getNombre().equals(nombre)) {
						String est = ci.getLEstados().get( i ).get(0);  // Nos vale cualquiera de los estados - eléctricamente son iguales
						ArrayList<ArrayList<Integer>> lConexions = c.getListaConexionesInt( est );
						for (ArrayList<Integer> conx : lConexions) {
							MarcasAverias.dibujaLinea( g, c.getAL().get(conx.get(0)), c.getAL().get(conx.get(1)), zoom );
						}
						break;
					}
				}
			} else if (ci!=null && ci.isCombinacionTodas()) {   // Opción "Todas"
				for (Componente c : VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().getComponentes()) {
					if (c.isInterruptor()) {
						ArrayList<String> lEsts = c.getListaEstadosInt();
						for (String est : lEsts) {
							ArrayList<ArrayList<Integer>> lConexions = c.getListaConexionesInt( est );
							for (ArrayList<Integer> conx : lConexions) {
								MarcasAverias.dibujaLinea( g, c.getAL().get(conx.get(0)), c.getAL().get(conx.get(1)), zoom );
							}
						}
					}
				}
			}
			g.setColor( col ); g.setStroke( st );
		}
		// Especial relés en modo edición de voltajes
		if (tipo.startsWith("Relé") && !VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().isModoEdicion()) { //$NON-NLS-1$
			int ci = VentanaEditorAveriaAuto.getLastVentana().getIndiceCombinacionSeleccionada();
			if (ci>=0 && !VentanaEditorAveriaAuto.getLastVentana().isCombinacionTodasSeleccionada()) {
				g.setColor( COLOR_INTERRUPTOR ); g.setStroke( STROKE_INTERRUPTOR );
				if (tipo.equals( "Relé4" )) {
					if (rele4Activado( ci )) {
						MarcasAverias.dibujaLinea( g, listaPuntos.get(2), listaPuntos.get(3), zoom );
					}
				} else if (tipo.equals( "Relé5" )) {
					if ("A".equals(rele5Activado( ci ))) {
						MarcasAverias.dibujaLinea( g, listaPuntos.get(2), listaPuntos.get(3), zoom );
					} else if ("B".equals(rele5Activado( ci ))) {
						MarcasAverias.dibujaLinea( g, listaPuntos.get(2), listaPuntos.get(4), zoom );
					}
				}
				g.setColor( col ); g.setStroke( st );
			} // No dibuja nada si la combinación es todas  //  else if (ci!=null && ci.isCombinacionTodas()) {   // Opción "Todas"
		}
		// Cálculo de rectángulo de componente y dibujado estándar
		int xMin = 10000; int xMax = -10000; int yMin = 10000; int yMax = -10000;
		for (Point p : listaPuntos) {
			if (p.x < xMin) xMin = p.x; if (p.x > xMax) xMax = p.x; if (p.y < yMin) yMin = p.y; if (p.y > yMax) yMax = p.y;
			if (pAnt != null && dibujadoEstandar) {
				MarcasAverias.dibujaLinea( g, pAnt,  p, zoom );
				dibujaCuadraditoEnMedio( g, p, pAnt, zoom );
			}
			pAnt = p;
		}
		// Especial electrónica
		if (tipo.equals("Elec")) { //$NON-NLS-1$
			Point p1 = MarcasAverias.aplicaZoom(new Point(xMin,yMin), zoom);
			Point p2 = MarcasAverias.aplicaZoom(new Point(xMax,yMax), zoom);
			MarcasAverias.dibujaRect( g, new Point(p1.x-6, p1.y-4), new Point(p2.x+6, p2.y+4), 1.0 );
			for (Point p : listaPuntos) {
				MarcasAverias.dibujaCirculo( g, p, 4, zoom );
			}
		} else if (getNumPuntos()>2) {
			// g.setStroke( stroke1 );
			Point p1 = MarcasAverias.aplicaZoom(new Point(xMin,yMin), zoom);
			Point p2 = MarcasAverias.aplicaZoom(new Point(xMax,yMax), zoom);
			MarcasAverias.dibujaRect( g, new Point(p1.x-6, p1.y-4), new Point(p2.x+6, p2.y+4), 1.0 );
		}
		// Especial interruptores a partir int3
		Point pCentro = new Point((xMin+xMax)/2,(yMin+yMax)/2);
		if (tipo.equals("Int3")) { //$NON-NLS-1$
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(1), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(2), pCentro, zoom );
			dibujaCirculitoEnMedio( g, pCentro, pCentro, zoom, MarcasAverias.ANCHO_MEDIO_MARCA_COMP*2, true );
			dibujaSimboloEnMedio( g, pCentro, pCentro, zoom, "Int2" ); //$NON-NLS-1$
		} else if (tipo.equals("Int4")) { //$NON-NLS-1$
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(1), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(2), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(3), pCentro, zoom );
			dibujaCirculitoEnMedio( g, pCentro, pCentro, zoom, MarcasAverias.ANCHO_MEDIO_MARCA_COMP*2, true );
			dibujaSimboloEnMedio( g, pCentro, pCentro, zoom, "Int2" ); //$NON-NLS-1$
		} else if (tipo.equals("Int5")) { //$NON-NLS-1$
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(1), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(2), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(3), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(4), pCentro, zoom );
			dibujaCirculitoEnMedio( g, pCentro, pCentro, zoom, MarcasAverias.ANCHO_MEDIO_MARCA_COMP*2, true );
			dibujaSimboloEnMedio( g, pCentro, pCentro, zoom, "Int2" ); //$NON-NLS-1$
		} else if (tipo.equals("Int6")) { //$NON-NLS-1$
			MarcasAverias.dibujaLinea( g, listaPuntos.get(0), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(1), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(2), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(3), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(4), pCentro, zoom );
			MarcasAverias.dibujaLinea( g, listaPuntos.get(5), pCentro, zoom );
			dibujaCirculitoEnMedio( g, pCentro, pCentro, zoom, MarcasAverias.ANCHO_MEDIO_MARCA_COMP*2, true );
			dibujaSimboloEnMedio( g, pCentro, pCentro, zoom, "Int2" ); //$NON-NLS-1$
		}
		// Avería
		if (averia) {
			g.setColor( Color.red ); g.setStroke( MarcasAverias.STROKE5 );
			MarcasAverias.dibujaCruz( g, new Point(xMin,yMin), new Point(xMax,yMax), 15, zoom );
		}
	}	
	
	public Rectangle getRectangulo(int margenExtraLateral, int margenExtraVertical) {
		int xMin = 10000; int xMax = -10000; int yMin = 10000; int yMax = -10000;
		for (Point p : listaPuntos) {
			if (p.x < xMin) xMin = p.x; if (p.x > xMax) xMax = p.x; if (p.y < yMin) yMin = p.y; if (p.y > yMax) yMax = p.y;
		}
		return new Rectangle( xMin-margenExtraLateral, yMin-margenExtraVertical, xMax-xMin+2*margenExtraLateral, yMax-yMin+2*margenExtraVertical );
	}

		private transient TextoDeCircuito textoAsociado = null;
		public void setTextoAsociado( TextoDeCircuito tdc ) { textoAsociado = tdc; }
		public TextoDeCircuito getTextoAsociado() { return textoAsociado; }
		
		private transient JPanel miPanelEdicion = null;
		private transient JTextField tfNombre = null;
		private transient JTextField tfConf1 = null;
		private transient JTextField tfConf2 = null;
		private transient JTextField tfConf3 = null;
		private transient JCheckBox cbAveria = null;
		private transient JCheckBox cbSeleccionable = null;
		private transient JComboBox<String> cbEstados = null;    //  Especial de interruptor 
		private transient ArrayList<String> lEstados = null;     //  Especial de interruptor
		private transient ArrayList<ArrayList<ArrayList<Integer>>> lConexiones = null;   //  Especial de interruptor
		private transient JToggleButton tbAnyadirPuntos = null;  //  Especial de la electrónica
		private transient JToggleButton tbQuitarPuntos = null;   //  Especial de la electrónica

		private static transient ControladorVentanaEAA.KeyListenerTeclado escTecladoEnTextosComp;
		static {
			try {
				escTecladoEnTextosComp = (ControladorVentanaEAA.KeyListenerTeclado) ControladorVentanaEAA.getLast().escTeclado.clone();
				escTecladoEnTextosComp.addTeclaNoGestionada( KeyEvent.VK_DELETE );
			} catch (CloneNotSupportedException e) {
			}
		}
		
		@Override
		public JPanel getPanelEdicion() {
			if (miPanelEdicion == null) {
				miPanelEdicion = new JPanel();
				miPanelEdicion.setMinimumSize( new Dimension( 100, 150 ));
				JPanel linea1 = new JPanel();
				JPanel linea2 = new JPanel();
				JPanel linea3 = new JPanel();
				JPanel linea4 = new JPanel();
				JPanel linea5 = new JPanel();
				miPanelEdicion.setLayout( new BoxLayout( miPanelEdicion, BoxLayout.Y_AXIS ));
				miPanelEdicion.add( linea1 );
				linea1.add( new JLabel( "[" + tipo + Messages.getString("Componente.141")) ); //$NON-NLS-1$ //$NON-NLS-2$
				tfNombre = new JTextField( 10 );
					tfNombre.setText( nombre );
					tfNombre.addKeyListener( ControladorVentanaEAA.getLast().escTecladoTextos );
					tfNombre.addKeyListener( escTecladoEnTextosComp );
				linea1.add( tfNombre );
				tfConf1 = null; tfConf2 = null; tfConf3 = null;
				if (isInterruptor()) {   // Interruptor
					miPanelEdicion.add( linea2 );
					miPanelEdicion.add( linea3 );
					tfConf1 = new JTextField( 15 ); tfConf2 = new JTextField( 15 );
					tfConf1.addKeyListener( ControladorVentanaEAA.getLast().escTecladoTextos );
					tfConf2.addKeyListener( ControladorVentanaEAA.getLast().escTecladoTextos );
					tfConf1.addKeyListener( escTecladoEnTextosComp );
					tfConf2.addKeyListener( escTecladoEnTextosComp );
						JLabel l1 = new JLabel( Messages.getString("Componente.142")); //$NON-NLS-1$
						l1.setToolTipText( Messages.getString("Componente.143")); //$NON-NLS-1$
					linea2.add( l1 );
					linea2.add( tfConf1 );
						initInterruptor();  // Inicializa cbEstados
						tfConf2.setEditable( false );
					linea2.add( cbEstados );
					JLabel l2 = new JLabel( Messages.getString("Componente.144") ); //$NON-NLS-1$
						l2.setToolTipText( Messages.getString("Componente.145")); //$NON-NLS-1$
					linea3.add( l2 );
					linea3.add( tfConf2 );
				} else if (tipo.equals("Elec")) {  // Electrónica //$NON-NLS-1$
					miPanelEdicion.add( linea2 );
					tbAnyadirPuntos = new JToggleButton( Messages.getString("Componente.147") ); //$NON-NLS-1$
					tbQuitarPuntos = new JToggleButton( Messages.getString("Componente.148") ); //$NON-NLS-1$
					linea2.add( tbAnyadirPuntos );
					linea2.add( tbQuitarPuntos );
					tbAnyadirPuntos.addActionListener( new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
						if (tbAnyadirPuntos.isSelected() && tbQuitarPuntos.isSelected()) tbQuitarPuntos.setSelected( false );
					} });
					tbQuitarPuntos.addActionListener( new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
						if (tbQuitarPuntos.isSelected() && tbAnyadirPuntos.isSelected()) tbAnyadirPuntos.setSelected( false );
					} });
				} else if (tipo.startsWith("Fusible") || tipo.startsWith("Batería") || tipo.startsWith("Masa") || tipo.startsWith("Lámpara") ||  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						   tipo.startsWith("Pot.alt.") || tipo.startsWith("Pot") || tipo.startsWith("Relé") || tipo.startsWith("Motor") ||  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						   tipo.startsWith("Bocina")) { //$NON-NLS-1$
					// No se añaden otras líneas de configuración
				} else {
					miPanelEdicion.add( linea2 );
					miPanelEdicion.add( linea3 );
					miPanelEdicion.add( linea4 );
					tfConf1 = new JTextField( 15 ); tfConf2 = new JTextField( 15 ); tfConf3 = new JTextField( 15 );
					tfConf1.addKeyListener( ControladorVentanaEAA.getLast().escTecladoTextos );
					tfConf2.addKeyListener( ControladorVentanaEAA.getLast().escTecladoTextos );
					tfConf3.addKeyListener( ControladorVentanaEAA.getLast().escTecladoTextos );
					tfConf1.addKeyListener( escTecladoEnTextosComp );
					tfConf2.addKeyListener( escTecladoEnTextosComp );
					tfConf3.addKeyListener( escTecladoEnTextosComp );
					linea2.add( new JLabel( Messages.getString("Componente.158")) ); //$NON-NLS-1$
					linea2.add( tfConf1 );
					linea3.add( new JLabel( Messages.getString("Componente.159")) ); //$NON-NLS-1$
					linea3.add( tfConf2 );
					linea4.add( new JLabel( Messages.getString("Componente.160")) ); //$NON-NLS-1$
					linea4.add( tfConf3 );
				}
				miPanelEdicion.add( linea5 );
				cbAveria = new JCheckBox( Messages.getString("Componente.161") ); //$NON-NLS-1$
					cbAveria.setSelected( averia );
					linea5.add( cbAveria );
				cbSeleccionable = new JCheckBox( Messages.getString("Componente.162") ); //$NON-NLS-1$
					cbSeleccionable.setSelected( seleccionable );
					linea5.add( cbSeleccionable );
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
						if (isInterruptor()) {  // Si pierde el foco el nombre es que ya no se está editando el interruptor
							if (VentanaEditorAveriaAuto.getLastVentana()!=null && ControladorVentanaEAA.getLast().getEstado() == ControladorVentanaEAA.Estado.INTERRUPTOR) {
								ControladorVentanaEAA.getLast().restauraAnteriorEstado();
								VentanaEditorAveriaAuto.getLastVentana().setMensaje( " " ); //$NON-NLS-1$
							}
						}
					}
					@Override
					public void focusGained(FocusEvent e) {
						antVal = tfNombre.getText();
					}
				});
				if (tfConf1!=null) tfConf1.addFocusListener( new FocusListener() {
					String antVal = null;
					@Override
					public void focusLost(FocusEvent e) {
						if (!tfConf1.getText().equals(antVal)) {  // Ha habido cambio de valor en el textfield
							String nuevoValor = tfConf1.getText();
							setConfig1( nuevoValor );
						}
					}
					@Override
					public void focusGained(FocusEvent e) {
						antVal = tfNombre.getText();
					}
				});
				if (tfConf2!=null) tfConf2.addFocusListener( new FocusListener() {
					String antVal = null;
					@Override
					public void focusLost(FocusEvent e) {
						if (!tfConf2.getText().equals(antVal)) {  // Ha habido cambio de valor en el textfield
							String nuevoValor = tfConf2.getText();
							setConfig2( nuevoValor );
						}
					}
					@Override
					public void focusGained(FocusEvent e) {
						antVal = tfNombre.getText();
					}
				});
				if (tfConf3!=null) tfConf3.addFocusListener( new FocusListener() {
					String antVal = null;
					@Override
					public void focusLost(FocusEvent e) {
						if (!tfConf3.getText().equals(antVal)) {  // Ha habido cambio de valor en el textfield
							String nuevoValor = tfConf3.getText();
							setConfig3( nuevoValor );
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
						ControladorVentanaEAA.getLast().redibujaCircuito();
					}
				});
				cbSeleccionable.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setSeleccionable( cbSeleccionable.isSelected() );
					}
				});
			}
			tfNombre.setText( nombre );
			if (tfConf1!=null) tfConf1.setText( config1 );
			if (tfConf2!=null) tfConf2.setText( config2 );
			if (tfConf3!=null) tfConf3.setText( config3 );
			return miPanelEdicion;
		}

		
// 	"Fusible", "Batería", "Masa", "Lámpara", "Pot.alt.luces", "Pot3", "Relé4", "Relé5", "Int2", "Int3", "Int4", "Int5", "Int6", "Motor", "Bocina"
		
		/** Devuelve la lista de puntos conectados desde un punto dado sobre el componente (si el punto no pertenece, la lista es vacía)
		 * @param p	Punto de tensión de entrada
		 * @param ci	Combinación de interrupción
		 * @param numCombinacion	Número de combinación (estado)
		 * @return	Lista de puntos, vacía si no está conectado con el punto indicado de entrada
		 */
		public ArrayList<Punto> getPuntosConectados( Punto p, CombinacionInterrupcion ci, int numCombinacion ) {
			ArrayList<Punto> props = new ArrayList<Punto>();
			if (tipo.startsWith("Fusible")) { //$NON-NLS-1$
				// Devuelve todos los puntos del componente que no son el de propagación indicado
				if (getAL().contains(p)) {
					for (Punto p2 : getAL()) {
						if (p2!=p) {
							props.add( p2 );
						}
					}
				}
			} else if (tipo.startsWith("Lámpara") || tipo.startsWith("Motor") || tipo.startsWith("Bocina")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// Devuelve todos los puntos del componente que no son el de propagación indicado
				// TODO Chequear si esto da algún problema. La lámpara, motor o bocina no propagan (si propagaran se van los voltajes donde no deben)
//				if (getAL().contains(p)) {
//					for (Punto p2 : getAL()) {
//						if (p2!=p) {
//							props.add( p2 );
//						}
//					}
//				}
			} else if (tipo.startsWith( "Int" )) { //$NON-NLS-1$
				if (getAL().contains(p)) {
					int miInterruptor = ci.getLInterruptores().indexOf( this );
					if (miInterruptor != -1) {
						String miEstado = ci.getLEstados().get(miInterruptor).get(0);  // Vale cualquier estado - son idénticos eléctricamente
						ArrayList<ArrayList<Integer>> cons = getListaConexionesInt( miEstado );
						for (ArrayList<Integer> con : cons) {
							if (listaPuntos.get(con.get(0))==p)
								props.add( listaPuntos.get(con.get(1)));
							else if (listaPuntos.get(con.get(1))==p)
								props.add( listaPuntos.get(con.get(0)));
						}
					}
				}
			}
			// System.out.println( "Propagados = " + props );
			return props;
		}

		/** Devuelve la lista de puntos propagados desde un punto dado sobre el componente (si el punto no pertenece, la lista es vacía)
		 * @param p	Punto de tensión a propagar
		 * @param ci	Combinación de interrupción
		 * @param numCombinacion	Número de combinación (estado)
		 * @return	Lista de puntos, vacía si no se propaga
		 */
		public ArrayList<Punto> getPuntosPropagados( Punto p, CombinacionInterrupcion ci, int numCombinacion ) {
			ArrayList<Punto> props = new ArrayList<Punto>();
			if (tipo.startsWith("Fusible")) { //$NON-NLS-1$
				// Devuelve todos los puntos del componente que no son el de propagación indicado
				if (getAL().contains(p)) {
					for (Punto p2 : getAL()) {
						if (p2!=p) {
							props.add( p2 );
						}
					}
				}
			} else if (tipo.startsWith( "Int" )) { //$NON-NLS-1$
				if (getAL().contains(p)) {
					int miInterruptor = ci.getLInterruptores().indexOf( this );
					if (miInterruptor != -1) {
						String miEstado = ci.getLEstados().get(miInterruptor).get(0);  // Vale cualquier estado - son idénticos eléctricamente
						ArrayList<ArrayList<Integer>> cons = getListaConexionesInt( miEstado );
						for (ArrayList<Integer> con : cons) {
							if (listaPuntos.get(con.get(0))==p)
								props.add( listaPuntos.get(con.get(1)));
							else if (listaPuntos.get(con.get(1))==p)
								props.add( listaPuntos.get(con.get(0)));
						}
					}
				}
			}
			return props;
		}

	/** Cambio de valor de configuración 1 (comportamiento especial para el caso de interruptor)
	 * @param conf	Nuevo valor de configuración
	 */
	public void setConfig1( String conf ) { 
		config1 = conf; 
		if (isInterruptor()) {   // Interruptor - caso especial de comportamiento de la configuración
			ArrayList<String> lEstadosAnt = lEstados;
			lEstados = new ArrayList<>();
			DefaultComboBoxModel<String> dcbm = new DefaultComboBoxModel<>();
			initInterruptor();
			StringTokenizer st = new StringTokenizer(config1,"#"); //$NON-NLS-1$
			while (st.hasMoreTokens()) {
				String tok = st.nextToken();
				lEstados.add( tok );
				dcbm.addElement( tok );
			}
			cbEstados.setModel( dcbm );
			if (lEstadosAnt==null || lEstados.size()!=lEstadosAnt.size()) {
				lConexiones = new ArrayList<ArrayList<ArrayList<Integer>>>();
				for (int i=0; i<lEstados.size(); i++) {
					lConexiones.add( new ArrayList<ArrayList<Integer>>() );
				}
				if (config2!=null && tfConf2!=null) {   // Han cambiado las posiciones de interrupción... reiniciando conexiones
					String str = ""; //$NON-NLS-1$
					for (int i=0; i<lEstados.size()-1; i++) str += "#"; //$NON-NLS-1$
					setConfig2( str );
					tfConf2.setText( config2 );
				}
			}
		}
	}
	/** Cambio de valor de configuración 2 (comportamiento especial para el caso de interruptor)
	 * @param conf	Nuevo valor de configuración
	 */
	public void setConfig2( String conf ) { 
		config2 = conf; 
		if (isInterruptor()) {   // Interruptor - caso especial de comportamiento de la configuración  (por si hay que hacer algo)
			lConexiones = getAllConexionesInterruptor();
		}
	}
	/** Cambio de valor de configuración 3
	 * @param conf	Nuevo valor de configuración
	 */
	public void setConfig3( String conf ) { 
		config3 = conf; 
	}

	
	// 
	// Métodos de electrónica
	//
	
	public boolean estaPulsadoElecAnyadir() {
		if (isElectronica()) {
			if (tbAnyadirPuntos==null) return false;
			return tbAnyadirPuntos.isSelected();
		}
		return false;
	}

	public boolean estaPulsadoElecQuitar() {
		if (isElectronica()) {
			if (tbQuitarPuntos==null) return false;
			return tbQuitarPuntos.isSelected();
		}
		return false;
	}
	
	// 
	// Métodos de interruptor
	//
	
	private void initInterruptor() {
		if (cbEstados==null) {
			cbEstados = new JComboBox<String>();
			cbEstados.setFocusable( false );
			// cbEstados.setEditable( false );
			cbEstados.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ControladorVentanaEAA.getLast().setEstado( ControladorVentanaEAA.Estado.INTERRUPTOR );
					VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().getPanelDibujo().repaint();
					VentanaEditorAveriaAuto.getLastVentana().setMensaje( Messages.getString("Componente.174") ); //$NON-NLS-1$
					tfNombre.requestFocus();
				}
			} );
			cbEstados.addComponentListener( new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
				}
				@Override
				public void componentHidden(ComponentEvent e) {
					if (ControladorVentanaEAA.getLast().getEstado() == ControladorVentanaEAA.Estado.INTERRUPTOR) {
						ControladorVentanaEAA.getLast().restauraAnteriorEstado();
						VentanaEditorAveriaAuto.getLastVentana().setMensaje( " " ); //$NON-NLS-1$
						VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().getPanelDibujo().repaint();
					}
				}
			});
			cbEstados.addKeyListener( ControladorVentanaEAA.getLast().escTecladoTextos );
			cbEstados.addKeyListener( escTecladoEnTextosComp );
		}
	}
	
	/** Devuelve la lista de estados de un interruptor
	 * @return	Lista de estados, null si el componente no es un interruptor
	 */
	public ArrayList<String> getListaEstadosInt() {
		if (isInterruptor())
			return lEstados;
		else
			return null;
	}
	
	/** Devuelve la lista de listas de estados diferentes de un interruptor
	 * @return	Lista de listas de estados diferentes, null si el componente no es un interruptor
	 * 			Cada sublista contiene todos los estados cuya composición eléctrica es la misma
	 */
	public ArrayList<ArrayList<String>> getListaEstadosDifsInt() {
		if (isInterruptor()) {
			ArrayList<ArrayList<String>> lRet = new ArrayList<>();
			ArrayList<String> estadosQuedan = new ArrayList<String>(lEstados);
			while (!estadosQuedan.isEmpty()) {
				ArrayList<String> estEquiv = new ArrayList<String>();
				String estado = estadosQuedan.get(0);
				estEquiv.add( estado );
				estadosQuedan.remove(0);
				ArrayList<ArrayList<Integer>> conexs = getConexionesInterruptorEnEstado( estado );
				for (int i=estadosQuedan.size()-1; i>=0; i--) {
					String estado2 = estadosQuedan.get(i);
					ArrayList<ArrayList<Integer>> conexs2 = getConexionesInterruptorEnEstado( estado2 );
					if (conexEquivalentes(conexs,conexs2)) {
						estEquiv.add( 1, estado2 );
						estadosQuedan.remove( i );
					}
				}
				lRet.add( estEquiv );
			}
			return lRet;
		} else
			return null;
	}
		private static boolean conexEquivalentes(ArrayList<ArrayList<Integer>> con1, ArrayList<ArrayList<Integer>> con2) {
			if (con1.size()!=con2.size()) return false;
			ArrayList<ArrayList<Integer>> copia2 = new ArrayList<>();
			for (ArrayList<Integer> c : con2) {
				ArrayList<Integer> cop = new ArrayList<Integer>();
				cop.add( c.get(0) );
				cop.add( c.get(1) );
				copia2.add( cop );
			}
			for (ArrayList<Integer> c : con1) {
				int laHay = copia2.indexOf( c );
				if (laHay!=-1) copia2.remove(laHay);
			}
			return (copia2.isEmpty());
		}

	
	/** Devuelve la lista de conexiones de un interruptor
	 * @return	Lista de lista de listas de conexiones, null si el componente no es un interruptor
	 */
	public ArrayList<ArrayList<ArrayList<Integer>>> getListaConexionesInt() {
		if (isInterruptor())
			return lConexiones;
		else
			return null;
	}
	
	/** Devuelve la lista de conexiones de un interruptor en un estado dado
	 * @return	Lista de listas de conexiones, null si el componente no es un interruptor o el estado es incorrecto
	 */
	public ArrayList<ArrayList<Integer>> getListaConexionesInt( String estado ) {
		if (isInterruptor()) {
			int posInt = lEstados.indexOf( estado );
			if (posInt==-1) return null;     // El estado es incorrecto
			return lConexiones.get(posInt);  // El estado es correcto
		} else
			return null;
	}
	
	
	/** Activa o desactiva la conexión de un interruptor para el estado seleccionado actualmente en ese interruptor.
	 * Si no es un interruptor, no hace nada
	 * @param inicio
	 * @param destino
	 */
	public void setConexionInterruptor( Punto inicio, Punto destino ) {
		if (isInterruptor() && cbEstados!=null && cbEstados.getSelectedIndex()>=0) {
			if (listaPuntos.contains( inicio ) && listaPuntos.contains( destino )) {
				int posInicio = listaPuntos.indexOf( inicio );
				int posDestino = listaPuntos.indexOf( destino );
				if (posInicio < posDestino)
					setConexionesInterruptor( posInicio, posDestino );
				else
					setConexionesInterruptor( posDestino, posInicio );
				VentanaEditorAveriaAuto.getLastVentana().getMarcasAverias().getPanelDibujo().repaint();
				// System.out.println( cbEstados.getSelectedIndex() );
				// System.out.println( inicio );
				// System.out.println( destino );
			}
		}
	}

	// Devuelve una lista de lista de listas de conexiones de interruptor calculadas desde config2
	private ArrayList<ArrayList<ArrayList<Integer>>> getAllConexionesInterruptor() {
		ArrayList<ArrayList<ArrayList<Integer>>> lisTot = new ArrayList<>();
		if (config2==null) return null;
		String ss = config2.replaceAll("#", " # "); //$NON-NLS-1$ //$NON-NLS-2$
		StringTokenizer stTot = new StringTokenizer( ss, "#" ); //$NON-NLS-1$
		while (stTot.hasMoreTokens()) {
			String cons = stTot.nextToken().trim();
			ArrayList<ArrayList<Integer>> lis = new ArrayList<>();
			StringTokenizer st = new StringTokenizer(cons,"+"); //$NON-NLS-1$
			while (st.hasMoreTokens()) {
				String con = st.nextToken();
				int guion = con.indexOf("-"); //$NON-NLS-1$
				if (guion>-1) {
					try {
						int pto1 = Integer.parseInt( con.substring(0,guion));
						int pto2 = Integer.parseInt( con.substring(guion+1));
						ArrayList<Integer> lCon = new ArrayList<>();
						lCon.add( pto1 ); lCon.add( pto2 );
						lis.add( lCon );
					} catch (NumberFormatException e) { }
				}
			}
			lisTot.add( lis );
		}
		while (lEstados.size() > lisTot.size()) lisTot.add( new ArrayList<ArrayList<Integer>>() );  // Posible error en entrada, para que sea consistente la longitud de ambas listas
		return lisTot;
	}
		
	// Devuelve una lista de listas de conexiones de interruptor para el estado seleccionado
	private ArrayList<ArrayList<Integer>> getConexionesInterruptor() {
		if (isInterruptor() && cbEstados!=null && cbEstados.getSelectedIndex()>=0) {
			return getConexionesInterruptorEnEstado( (String) cbEstados.getSelectedItem() );
		} else {
			return new ArrayList<ArrayList<Integer>>();
		}
	}
		
	// Devuelve una lista de listas de conexiones de interruptor para el estado seleccionado
	public ArrayList<ArrayList<Integer>> getConexionesInterruptorEnEstado( String estado ) {
		ArrayList<ArrayList<Integer>> lis = new ArrayList<>();
		if (isInterruptor() && estado!=null && cbEstados.getSelectedIndex()>=0) {
			int estadoInt = lEstados.indexOf( estado );
			if (estadoInt!=-1) {
				String cons = getStringConexionesInterruptor(estadoInt);
				StringTokenizer st = new StringTokenizer(cons,"+"); //$NON-NLS-1$
				while (st.hasMoreTokens()) {
					String con = st.nextToken();
					int guion = con.indexOf("-"); //$NON-NLS-1$
					if (guion>-1) {
						try {
							int pto1 = Integer.parseInt( con.substring(0,guion) );
							int pto2 = Integer.parseInt( con.substring(guion+1) );
							ArrayList<Integer> lCon = new ArrayList<>();
							lCon.add( pto1 ); lCon.add( pto2 );
							lis.add( lCon );
						} catch (NumberFormatException e) { }
					}
				}
			}
		}
		return lis;
	}
			
	// Modifica el string de configuración (y el estado interno) de conexión del interruptor para el estado seleccionado. Pto1 < Pto2
	private void setConexionesInterruptor(int pto1, int pto2) {
		if (isInterruptor() && cbEstados!=null && cbEstados.getSelectedIndex()>=0) {
			int estadoInt = cbEstados.getSelectedIndex();
			String cons = getStringConexionesInterruptor(estadoInt);
			String estaCon = pto1 + "-" + pto2; //$NON-NLS-1$
			ArrayList<Integer> lConex = new ArrayList<Integer>();
			lConex.add( pto1 ); lConex.add( pto2 );
			if (cons.contains( estaCon )) {   // Ya estaba: quitar
				if (cons.equals(estaCon)) cons = ""; //$NON-NLS-1$
				else
					if (cons.startsWith( estaCon )) cons = cons.replace( estaCon+"+", "" ); //$NON-NLS-1$ //$NON-NLS-2$
					else cons = cons.replace( "+"+estaCon, "" ); //$NON-NLS-1$ //$NON-NLS-2$
				lConexiones.get(estadoInt).remove( lConex );
			} else {  // No estaba: poner
				if (cons.equals("")) cons = estaCon; //$NON-NLS-1$
				else cons = cons + "+" + estaCon; //$NON-NLS-1$
				lConexiones.get(estadoInt).add( lConex );
			}
			setStringConexionesInterruptor(estadoInt,cons);
		}
		// System.out.println( "CONEXXXX-- " + pto1 + " + " + pto2 + " ---> " + lEstados + " -- " + lConexiones );
	}
		
		private String getStringConexionesInterruptor( int numEst ) {
			int posi1 = -1;
			while (numEst>0) {
				posi1 = config2.indexOf("#",posi1+1); //$NON-NLS-1$
				if (posi1==-1) return ""; //$NON-NLS-1$
				numEst--;
			}
			int posi2 = config2.indexOf("#",posi1+1); //$NON-NLS-1$
			if (posi2 == -1) posi2 = config2.length();
			return config2.substring( posi1+1, posi2 );
		}
	
		private void setStringConexionesInterruptor( int numEst, String nuevoSt ) {
			int posi1 = -1;
			while (numEst>0) {
				posi1 = config2.indexOf("#",posi1+1); //$NON-NLS-1$
				if (posi1==-1) return;
				numEst--;
			}
			int posi2 = config2.indexOf("#",posi1+1); //$NON-NLS-1$
			if (posi2 == -1) posi2 = config2.length();
			setConfig2( config2.substring(0,posi1+1) + nuevoSt + config2.substring( posi2 ) );
			tfConf2.setText( config2 );
		}

	//
	// Métodos de Relé
	//
		
	/** Informa de la activación del relé4
	 * @param numCombinacion	Combinación de interrupción a comprobar
	 * @return	true si hay activación del relé (diferencia de tensiones en sus puntos 0 y 1), false en caso contrario
	 */
	public boolean rele4Activado( int numCombinacion ) {
		if (!tipo.equals("Relé4")) return false;
		if (listaPuntos.get(0).isVoltajeDefinido( numCombinacion ) && listaPuntos.get(1).isVoltajeDefinido( numCombinacion )) {
			double volt1 = listaPuntos.get(0).getVoltaje( numCombinacion );
			double volt2 = listaPuntos.get(1).getVoltaje( numCombinacion );
			if (volt1!=volt2) {   // Si son voltajes distintos, circula y entonces se cierra el interruptor
				return true;
			}
		}
		return false;
	}
		
	/** Informa de la activación del relé5
	 * @param numCombinacion	Combinación de interrupción a comprobar
	 * @return	"B" si hay activación del relé (diferencia de tensiones en sus puntos 0 y 1), "A" si hay la misma tensión, null en otro caso
	 */
	public String rele5Activado( int numCombinacion ) {
		if (!tipo.equals("Relé4")) return null;
		if (listaPuntos.get(0).isVoltajeDefinido( numCombinacion ) && listaPuntos.get(1).isVoltajeDefinido( numCombinacion )) {
			double volt1 = listaPuntos.get(0).getVoltaje( numCombinacion );
			double volt2 = listaPuntos.get(1).getVoltaje( numCombinacion );
			if (volt1!=volt2) {   // Si son voltajes distintos, circula y entonces se pone el interruptor en posición B (3-5)
				return "B";
			} else {  // Si no, entonces se pone el interruptor en posición A (3-4)
				return "A";
			}
		}
		return null;
	}
		
}

