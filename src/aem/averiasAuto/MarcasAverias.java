package aem.averiasAuto;

import static aem.averiasAuto.EditorAveriasAuto.logger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import aem.averiasAuto.elem.*;

public class MarcasAverias {
	private ArrayList<Punto> lPuntos;                 // Puntos gráficos
		private ArrayList<TextoDeCircuito> tPuntos;   // Textos de puntos
	private ArrayList<Conexion> lConexiones;          // Conexiones entre puntos gráficos (al menos 2 puntos cada conexión)
	private ArrayList<Conector> lConectores;          // Conexiones entre puntos gráficos (al menos 2 puntos cada conector)
	private ArrayList<Componente> lComponentes;       // Conexiones especiales por componente, entre varios puntos gráficos (2 puntos o más cada componente)
	private ArrayList<Resistencia> lResistencias;     // Conexiones especiales de resistencia, entre varios puntos gráficos (2 puntos por resistencia)
	String nomFicheroCircuito;                        // Nombre de fichero gráfico del circuito de fondo
	private boolean modoEdicion = true;               // true = modo edición. false = modo averías (cálculo de tensiones y resistencias, definición de averías)
	private boolean modoResistencias = false;         // true = modo resistencias. false = no
	private ArrayList<CombinacionInterrupcion> combsInterrupcion = new ArrayList<CombinacionInterrupcion>();   // Combinaciones de interrupción
	private CombinacionInterrupcion[] combsInterrupcionArray = null;
	
	private HashSet<ObjetoDeGrafico> conjObjetosSeleccionados;    // Lista de objetos seleccionados
	
	public static Stroke STROKE1 = new BasicStroke(1);
	public static Stroke STROKE3 = new BasicStroke(3);
	public static Stroke STROKE5 = new BasicStroke(5);
	public static Stroke STROKE_SELECCION = new BasicStroke(7);
	private Point marcaPunto = null;
	private Point marcaLinea = null;
	private Point marcaRect = null;
	
	private PanelDeDibujo pDibujo = null;

	// public static int RADIO_TEXTO_COMPONENTE = 20;
	// public static int DIST_MINIMA_PUNTOS_COMPONENTE = 26;
	public static int RADIO_MARCA_PUNTO = 4;
	public static int RADIO_CONEXION = 4;
	public static int RADIO_MARCA_RESISTENCIA = 6;
	public static int ANCHO_MEDIO_MARCA_COMP = 7;
	public static int CERCANIA_PUNTOS = 10;  // Si dos puntos están a esta distancia o menor, se consideran el mismo
	public static int CERCANIA_PUNTOS_MAYOR = 20;  // Si dos puntos están a esta distancia o menor, se consideran el mismo
	public static int CERCANIA_SELECCION_CONS = 30;  // Cercanía de selección de conectores o conexiones
	public static int CERCANIA_INCLUSION_PUNTOS = 3;  // Cercanía de punto a conexión para su inclusión
	public static int DIST_X_TEXTO = 10;    // Distancia x,y del punto a su texto. Anchura y altura de la caja
		public static int DIST_Y_TEXTO = -10;
		public static int ANCH_TEXTO_INICIAL = 40, ALT_TEXTO = 20, ANCH_TEXTO_POR_CAR = 7, ANCH_TEXTO_MINIMA = 5;   // Píxels
		public static int DIST_X_TEXTO_VERT = -25, DIST_Y_TEXTO_VERT = -30;   // Distancias en vertical
	private static Color COLOR_SELECCION = new Color( 255, 200, 0, 150);  // Color.orange with transparence
	
	private static String PREFIJO_PUNTO_STD = "P"; //$NON-NLS-1$
	
		
	/** Crea el objeto de datos de dibujo del circuito
	 * @param nombreFic	Nombre del fichero gráfico de fondo
	 */
	public MarcasAverias( String nombreFic ) {
		nomFicheroCircuito = nombreFic;
		lPuntos = new ArrayList<Punto>();
		tPuntos = new ArrayList<TextoDeCircuito>();
		lConexiones = new ArrayList<Conexion>();
		lConectores = new ArrayList<Conector>();
		lComponentes = new ArrayList<Componente>();
		lResistencias = new ArrayList<Resistencia>();
		conjObjetosSeleccionados = new HashSet<ObjetoDeGrafico>();
		Acciones.getInstance().reset( this );  // Inicia las acciones
	}
	
	/** Cambia o refresca el panel de dibujo asociado a estos datos. Inserta en el panel todos los componentes (textfields)
	 * @param pDibujo
	 */
	public void setPanelDibujo( PanelDeDibujo pDibujo ) {
		this.pDibujo = pDibujo;
		if (pDibujo!=null && tPuntos.size()>0) {
			for (TextoDeCircuito tf : tPuntos) pDibujo.add( tf );
			TextoDeCircuito.reloadPanel( pDibujo );
		}
	}
	
	public PanelDeDibujo getPanelDibujo() { return pDibujo; }
	
	public ArrayList<Punto> getPuntos() { return lPuntos; }
	public ArrayList<TextoDeCircuito> getTextos() { return tPuntos; }
	public ArrayList<Conexion> getConexiones() { return lConexiones; } 
	public ArrayList<Conector> getConectores() { return lConectores; } 
	public ArrayList<Componente> getComponentes() { return lComponentes; }
	public ArrayList<Resistencia> getResistencias() { return lResistencias; }
	
	/** Activa o desactiva el modo edición. Al activar el modo de edición de averías, crea las combinaciones de interrupción pertinentes
	 * @param modoEdi	true para el modo edición de circuito, false para el modo edición de tensiones, resistencias y averías
	 */
	public void setModoEdicion( boolean modoEdi ) {
		modoEdicion = modoEdi;
		if (!modoEdicion) {  // Entra en modo edición de averías
			clearCombsInterrupcion();
			// Buscar interruptores
			ArrayList<Componente> interruptores = new ArrayList<Componente>();
			ArrayList<ArrayList<ArrayList<String>>> interrupciones = new ArrayList<ArrayList<ArrayList<String>>>();
			for (Componente c : lComponentes) {
				if (c.getTipo().startsWith("Int")) { //$NON-NLS-1$
					interruptores.add( c );
					ArrayList<ArrayList<String>> conmutaciones = c.getListaEstadosDifsInt();
					interrupciones.add( conmutaciones );
				}
			}
			// Generar combinaciones
			// System.out.println( "Combinaciones!!" );
			// System.out.println( interruptores );
			// System.out.println( interrupciones );
			int numInts = interruptores.size();
			if (numInts==0) {
				int numTotalCombis = 1;
				CombinacionInterrupcion ci = new CombinacionInterrupcion();
				addCombInterrupcion( ci );
				// Regenerar voltajes (si procede)
				for (Punto p : lPuntos) {
					if (p.getVoltajes().size()!=numTotalCombis)
						p.initVoltajes( numTotalCombis );
				}
			} else {
				int[] conts = new int[numInts];  // contadores - empiezan todos a 0
				int numCombi = 0;
				int numTotalCombis = interrupciones.get(0).size();
				for (int i=1; i<conts.length; i++) numTotalCombis *= interrupciones.get(i).size();
				// System.out.println( numTotalCombis );
				while (numCombi < numTotalCombis) {
					// Añadir combinación
					CombinacionInterrupcion ci = new CombinacionInterrupcion( interruptores.get(0), interrupciones.get(0).get(conts[0]) );
					for (int k=1; k<numInts; k++) {
						ci.addCombinacion( interruptores.get(k), interrupciones.get(k).get(conts[k]) );
					}
					addCombInterrupcion( ci );
					// System.out.println( "  " + ci );
					// Buscar siguiente
					int j = numInts-1;
					conts[j]++;
					while (j>=0 && conts[j]>=interrupciones.get(j).size()) {
						conts[j] = 0;
						j = j - 1;
						if (j>=0) conts[j]++;
					}
					numCombi++;
				}
				// Regenerar voltajes (si procede)
				for (Punto p : lPuntos) {
					if (p.getVoltajes().size()!=numTotalCombis)
						p.initVoltajes( numTotalCombis );
				}
			}
			pDibujo.repaint();
		}
	}
	
	/** Devuelve el modo de edición
	 * @return	true si está en modo de edición de circuito, false para el modo edición de tensiones, resistencias y averías
	 */
	public boolean isModoEdicion() { return modoEdicion; }
	
	/** Activa o desactiva el modo de resistencias.
	 * @param modoEdi	true para el modo de resistencias, false no.
	 */
	public void setModoResistencias( boolean modoRes ) {
		modoResistencias = modoRes;
		pDibujo.repaint();
	}

	/** Devuelve el modo de edición
	 * @return	true si está en modo de edición de resistencias, false en caso contrario
	 */
	public boolean isModoResistencias() { return modoResistencias; }
	
	/** Añade una nueva combinación de interrupción
	 * @param nuevaComb	Combinación de interrupción
	 */
	public void addCombInterrupcion( CombinacionInterrupcion nuevaComb ) {
		combsInterrupcion.add( nuevaComb );
	}
	
	/** Añade una nueva combinación de interrupción
	 * @param nuevaComb	Combinación de interrupción. Formato: nombreIntNumSal | nombreIntNumSal ...
	 */
	public void addCombInterrupcion( String nuevaComb ) {
		try {
			CombinacionInterrupcion ci = new CombinacionInterrupcion( nuevaComb, this );
			combsInterrupcion.add( ci );
		} catch (NullPointerException e) {
			System.out.println( Messages.getString("MarcasAverias.2") + nuevaComb ); //$NON-NLS-1$
			e.printStackTrace();
		}
	}
	
	/** Reinicia las combinaciones de interrupción
	 */
	public void clearCombsInterrupcion() {
		combsInterrupcion = new ArrayList<CombinacionInterrupcion>();
		combsInterrupcionArray = null;
	}
	
	/** Devuelve las combinaciones de interrupción
	 * @return	Lista de combinaciones
	 */
	public ArrayList<CombinacionInterrupcion> getCombsInterrupcion() {
		return combsInterrupcion;
	}
	
	/** Devuelve las combinaciones de interrupción en un array
	 * @return	Array de combinaciones
	 */
	public CombinacionInterrupcion[] getCombsInterrupcionArray() {
		if (combsInterrupcionArray == null || combsInterrupcionArray.length != combsInterrupcion.size() ) {
			combsInterrupcionArray = new CombinacionInterrupcion[ combsInterrupcion.size() + 1 ];
			for (int i=0; i<combsInterrupcion.size(); i++)
				combsInterrupcionArray[i] = combsInterrupcion.get(i);
			combsInterrupcionArray[combsInterrupcion.size()] = new CombinacionInterrupcion();
		}
		return combsInterrupcionArray;
	}
	
	/** Recalcula las tensiones de todo el circuito
	 */
	public void recalculaTensiones() {
		// Para cada configuración de interruptores
		int numCombinacion;
		// Previo: quitar los voltajes que no han sido editados a mano
		for (numCombinacion=0; numCombinacion<combsInterrupcion.size(); numCombinacion++) {
			for (Punto p : lPuntos) {
				if (!p.getVoltajeEditado(numCombinacion)) {
					p.setVoltaje( numCombinacion, Punto.VOLTAJE_INDEFINIDO );
					p.setVoltajeEditado( numCombinacion, false );  // Editado automático
				}
			}
		}
		numCombinacion = 0;
		for (CombinacionInterrupcion ci : combsInterrupcion) {
			// Primera iteración: propagar tensiones directas
			// Empezar por los puntos que ya tienen asignada tensión
			for (Punto p : lPuntos) {
				if (p.isVoltajeDefinido(numCombinacion)) {
					propagaTension( p, Double.MAX_VALUE, ci, numCombinacion, true );
				}
			}
			// Seguir por baterías y tierras
			for (Componente c : lComponentes) {
				if (c.getTipo().startsWith("Bate")) { //$NON-NLS-1$
					for (Punto p : c.getAL()) {
						if (p.getNombre().endsWith( "G" ) | p.getNombre().endsWith( "G1" ) | p.getNombre().endsWith( "G2" ) | p.getNombre().endsWith( "G3" ) | p.getNombre().endsWith( "G4" )) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
							// Masa
							propagaTension( p, 0, ci, numCombinacion, true );
						} else {
							if (p.getNombre().endsWith( "12" )) { //$NON-NLS-1$
								propagaTension( p, 12, ci, numCombinacion, true );
							}
						}
					}
				} else if (c.getTipo().startsWith( "Masa" )) { //$NON-NLS-1$
					for (Punto p : c.getAL()) {
						propagaTension( p, 0, ci, numCombinacion, true );
					}
				}
			}
			
			// Segunda iteración: propagar tensiones si no llega diferencia de tensión 
			for (Punto p : lPuntos) {
				if (p.isVoltajeDefinido(numCombinacion)) {
					propagaTension( p, Double.MAX_VALUE, ci, numCombinacion, false );
				}
			}

			// Tercera iteración: opera con los relés
			for (Componente c : lComponentes) {
				if (c.getTipo().equals("Relé4")) {
					if (c.rele4Activado( numCombinacion )) {   // Si son voltajes distintos, circula y entonces se cierra el interruptor
						if (c.getAL().get(2).isVoltajeDefinido( numCombinacion ) && !c.getAL().get(3).isVoltajeDefinido( numCombinacion )) {
							propagaTension( c.getAL().get(3), c.getAL().get(2).getVoltaje( numCombinacion ), ci, numCombinacion, false );  // Propaga 3 al 4
						} else if (c.getAL().get(3).isVoltajeDefinido( numCombinacion ) && !c.getAL().get(2).isVoltajeDefinido( numCombinacion )) {
							propagaTension( c.getAL().get(2), c.getAL().get(3).getVoltaje( numCombinacion ), ci, numCombinacion, false );  // Propaga 4 al 3
						}
					}
				} else if (c.getTipo().equals("Relé5")) {
					if ("B".equals(c.rele5Activado( numCombinacion ))) {   // Si son voltajes distintos, circula y entonces se pone el interruptor en posición B (3-5)
						if (c.getAL().get(2).isVoltajeDefinido( numCombinacion ) && !c.getAL().get(4).isVoltajeDefinido( numCombinacion )) {
							propagaTension( c.getAL().get(4), c.getAL().get(2).getVoltaje( numCombinacion ), ci, numCombinacion, false );  // Propaga 3 al 4
						} else if (c.getAL().get(4).isVoltajeDefinido( numCombinacion ) && !c.getAL().get(2).isVoltajeDefinido( numCombinacion )) {
							propagaTension( c.getAL().get(2), c.getAL().get(4).getVoltaje( numCombinacion ), ci, numCombinacion, false );  // Propaga 4 al 3
						}
					} else if ("A".equals(c.rele5Activado( numCombinacion ))) {   // Si son voltajes iguales, se pone el interruptor en posición A (3-4)
						if (c.getAL().get(2).isVoltajeDefinido( numCombinacion ) && !c.getAL().get(3).isVoltajeDefinido( numCombinacion )) {
							propagaTension( c.getAL().get(3), c.getAL().get(2).getVoltaje( numCombinacion ), ci, numCombinacion, false );  // Propaga 3 al 4
						} else if (c.getAL().get(3).isVoltajeDefinido( numCombinacion ) && !c.getAL().get(2).isVoltajeDefinido( numCombinacion )) {
							propagaTension( c.getAL().get(2), c.getAL().get(3).getVoltaje( numCombinacion ), ci, numCombinacion, false );  // Propaga 4 al 3
						}
					}
				}
			}
			
			// Cuarta iteración: marca puntos sin conectar (PNC)
			for (Punto p : lPuntos) {
				if (!p.isVoltajeDefinido(numCombinacion)) {
					if (!p.getVoltajeEditado(numCombinacion))
						p.setVoltaje( numCombinacion, Punto.VOLTAJE_PNC );
						p.setVoltajeEditado( numCombinacion, false );  // Editado automático
				}
			}
			
			numCombinacion++;
		}
	}

		// si tension==Double.MAX_VALUE coge la tensión que tenga ya el punto
		// soloPropaga a true si solo se propaga la tensión buscando diferencias de potencial. false si se propaga por puntos conectados desactivados (por ejemplo una bombilla a la que solo llega tensión a un punto)
		private void propagaTension( Punto p, double tension, CombinacionInterrupcion ci, int numCombinacion, boolean soloPropaga ) {
			if (tension==Double.MAX_VALUE) tension = p.getVoltaje(numCombinacion);
			// Calcular puntos propagados para la combinación dada
			ArrayList<Punto> propagados = new ArrayList<Punto>();
			ArrayList<Punto> nuevos = new ArrayList<Punto>();
			ArrayList<Punto> nuevos2 = new ArrayList<Punto>();
			propagados.add( p );
			nuevos.add( p );
			do {
				for (Punto nuevo : nuevos) {
					for (Conexion c : lConexiones) {  // Propagar por conexiones
						if (c.getAL().contains(nuevo)) {
							for (Punto p2 : c.getAL()) {
								if (!propagados.contains(p2)) {
									if (p2.getVoltaje(numCombinacion)==Punto.VOLTAJE_INDEFINIDO || p2.getVoltaje(numCombinacion)==Punto.VOLTAJE_PNC) {
										propagados.add(p2);
										nuevos2.add(p2);
									}
								}
							}
						}
					}
				}
				for (Punto nuevo2 : nuevos) {
					for (Componente c : lComponentes) {  // Propagar por componente
						ArrayList<Punto> props = c.getPuntosConectados( nuevo2, ci, numCombinacion );
						for (Punto p2 : props) {
							if (!propagados.contains(p2)) {
								if (p2.getVoltaje(numCombinacion)==Punto.VOLTAJE_INDEFINIDO || p2.getVoltaje(numCombinacion)==Punto.VOLTAJE_PNC) {
									propagados.add(p2);
									nuevos2.add(p2);
								}
							}
						}
					}
				}
				nuevos = nuevos2;
				nuevos2 = new ArrayList<Punto>();
			} while (nuevos.size()>0);
			// Marcar la tensión de esos puntos (solo para los que no han sido editados a mano, o son indefinidos o PNC)
			for (Punto pto : propagados) {
				if (pto.getVoltajeEditado( numCombinacion) && pto.getVoltaje(numCombinacion)!=Punto.VOLTAJE_INDEFINIDO && pto.getVoltaje(numCombinacion)!=Punto.VOLTAJE_PNC ) {
					if (pto.getVoltaje( numCombinacion )!=tension) {
						String mens = Messages.getString("MarcasAverias.11") + pto.getNombre() + Messages.getString("MarcasAverias.12") +  //$NON-NLS-1$ //$NON-NLS-2$
								tension + Messages.getString("MarcasAverias.13") + pto.getVoltaje(numCombinacion); //$NON-NLS-1$
						JOptionPane.showMessageDialog( null, mens, Messages.getString("MarcasAverias.14"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$
					}
				} else {
					pto.setVoltaje( numCombinacion, tension );
					pto.setVoltajeEditado( numCombinacion, false );  // Editado automático
				}
			}
		}

		// RESISTENCIAS
		
		// Método para ver si una resistencia ya existe
		/** Comprueba si una resistencia ya existe
		 * @param p1	Punto 1 de la resistencia
		 * @param p2	Punto 2 de la resistencia
		 * @return	null si la resistencia no existe, la resistencia en cuestión si ya existe (entre los dos puntos indicados, en cualquier orden)
		 */
		public Resistencia yaExisteResistencia( Punto p1, Punto p2 ) {
			for (Resistencia r : getResistencias()) {
				if ((p1==r.getAL().get(0) && p2==r.getAL().get(1)) || (p1==r.getAL().get(1) && p2==r.getAL().get(0)))
					return r;
			}
			return null;
		}

		/** Recalcula las resistencias de todo el circuito
		 */
		public void recalculaResistencias() {
			// Para cada configuración de interruptores
			int numCombinacion;
			// Previo: quitar las resistencias que no han sido editadas a mano
			for (numCombinacion=0; numCombinacion<combsInterrupcion.size(); numCombinacion++) {
				for (Resistencia r : lResistencias) {
					if (!r.getResistenciaEditada(numCombinacion)) {
						r.setResistencia( numCombinacion, Resistencia.RESISTENCIA_INDEFINIDA );
						r.setResistenciaEditada( numCombinacion, false );
					}
				}
			}
			numCombinacion = 0;
			for (CombinacionInterrupcion ci : combsInterrupcion) {
				// Asignar resistencia a conexiones
				for (Conexion c : lConexiones) {
					Punto pIni = null;
					for (Punto p : c.getAL()) {
						if (pIni==null) {
							pIni = p;
						} else {
							Resistencia nuevaR = yaExisteResistencia( pIni, p );
							if (nuevaR==null) {  // La resistencia no existe - hay que crearla
								Acciones.getInstance().hacer( Messages.getString("MarcasAverias.15") ); //$NON-NLS-1$
								nuevaR = addResistencia( pIni, p );
								nuevaR.initResistencias( getCombsInterrupcion().size() );
							}
							if (!nuevaR.getResistenciaEditada( numCombinacion )) {
								nuevaR.setResistencia( numCombinacion, Resistencia.RESISTENCIA_CERO );
								nuevaR.setResistenciaEditada( numCombinacion, false );
							}
						}
					}
				}
				// Seguir por componentes
				for (Componente c : lComponentes) {
					// "Fusible", "Masa", "Lámpara", "Motor", "Bocina"
					if ("#Fusible#Masa#Lámpara#Motor#Bocina".contains(c.getTipo())) { //$NON-NLS-1$
						Punto pIni = null;
						for (Punto p : c.getAL()) {
							if (pIni==null) {
								pIni = p;
							} else {
								Resistencia nuevaR = yaExisteResistencia( pIni, p );
								if (nuevaR==null) {  // La resistencia no existe - hay que crearla
									Acciones.getInstance().hacer( Messages.getString("MarcasAverias.0") ); //$NON-NLS-1$
									nuevaR = addResistencia( pIni, p );
									nuevaR.initResistencias( getCombsInterrupcion().size() );
								}
								if (!nuevaR.getResistenciaEditada( numCombinacion )) {
									nuevaR.setResistencia( numCombinacion, Resistencia.RESISTENCIA_CERO );
									nuevaR.setResistenciaEditada( numCombinacion, false );
								}
							}
						}
					} else if (c.getTipo().startsWith("Int")) { //$NON-NLS-1$
						// Interruptores
						ArrayList<String> estadosInterruptor = ci.getLEstadosDeInt(c);
						for (String estado : estadosInterruptor) {
							ArrayList<ArrayList<Integer>> puntos = c.getConexionesInterruptorEnEstado( estado );
							for (ArrayList<Integer> conexion : puntos) {
								Punto p1 = c.getAL().get( conexion.get(0) );
								Punto p2 = c.getAL().get( conexion.get(1) );
								Resistencia nuevaR = yaExisteResistencia( p1, p2 );
								if (nuevaR==null) {  // La resistencia no existe - hay que crearla
									Acciones.getInstance().hacer( "Añadir resistencia" ); //$NON-NLS-1$
									nuevaR = addResistencia( p1, p2 );
									nuevaR.initResistencias( combsInterrupcion.size() );
								}
								for (int numC=0; numC < combsInterrupcion.size(); numC++) {
									if (numC==numCombinacion && !nuevaR.getResistenciaEditada( numCombinacion )) {
										nuevaR.setResistencia( numC, Resistencia.RESISTENCIA_CERO );  // Pone 0 en ese estado de interrupción
										nuevaR.setResistenciaEditada( numC, false );  // Es cálculo automático
									} else if (!nuevaR.getResistenciaEditada( numC ) && nuevaR.getResistencia(numC)==Resistencia.RESISTENCIA_INDEFINIDA) {
										nuevaR.setResistencia( numC, Resistencia.RESISTENCIA_INFINITA );  // E infinito en el resto de estados
										nuevaR.setResistenciaEditada( numC, false );  // Es cálculo automático
									}
								}
							}
						}
					} else if (c.getTipo().startsWith("Relé")) { //$NON-NLS-1$
						// Relés
						if (c.getTipo().equals("Relé4")) {
							Punto p1 = c.getAL().get( 2 );
							Punto p2 = c.getAL().get( 3 );
							Resistencia nuevaR = yaExisteResistencia( p1, p2 );
							if (nuevaR==null) {  // La resistencia no existe - hay que crearla
								Acciones.getInstance().hacer( "Añadir resistencia" ); //$NON-NLS-1$
								nuevaR = addResistencia( p1, p2 );
								nuevaR.initResistencias( combsInterrupcion.size() );
							}
							if (c.rele4Activado( numCombinacion )) {
								nuevaR.setResistencia( numCombinacion, Resistencia.RESISTENCIA_CERO );  // Pone 0 en ese estado de interrupción
								nuevaR.setResistenciaEditada( numCombinacion, false );  // Es cálculo automático
							} else {
								nuevaR.setResistencia( numCombinacion, Resistencia.RESISTENCIA_INFINITA );  // E infinito en el resto de estados
								nuevaR.setResistenciaEditada( numCombinacion, false );  // Es cálculo automático
							}
						} else if (c.getTipo().equals("Relé5")) {
							Punto p3 = c.getAL().get( 2 );
							Punto p4 = c.getAL().get( 3 );
							Punto p5 = c.getAL().get( 4 );
							Resistencia nuevaRA = yaExisteResistencia( p3, p4 );
							Resistencia nuevaRB = yaExisteResistencia( p3, p5 );
							if (nuevaRA==null) {  // La resistencia no existe - hay que crearla
								Acciones.getInstance().hacer( "Añadir resistencia" ); //$NON-NLS-1$
								nuevaRA = addResistencia( p3, p4 );
								nuevaRA.initResistencias( combsInterrupcion.size() );
							}
							if (nuevaRB==null) {  // La resistencia no existe - hay que crearla
								Acciones.getInstance().hacer( "Añadir resistencia" ); //$NON-NLS-1$
								nuevaRB = addResistencia( p3, p5 );
								nuevaRB.initResistencias( combsInterrupcion.size() );
							}
							if ("A".equals(c.rele5Activado( numCombinacion ))) {
								nuevaRA.setResistencia( numCombinacion, Resistencia.RESISTENCIA_CERO );
								nuevaRA.setResistenciaEditada( numCombinacion, false );  // Es cálculo automático
								nuevaRB.setResistencia( numCombinacion, Resistencia.RESISTENCIA_INFINITA );
								nuevaRB.setResistenciaEditada( numCombinacion, false );  // Es cálculo automático
							} else if ("B".equals(c.rele5Activado( numCombinacion ))) {
								nuevaRA.setResistencia( numCombinacion, Resistencia.RESISTENCIA_INFINITA );
								nuevaRA.setResistenciaEditada( numCombinacion, false );  // Es cálculo automático
								nuevaRB.setResistencia( numCombinacion, Resistencia.RESISTENCIA_CERO );  // Pone 0 en ese estado de interrupción
								nuevaRB.setResistenciaEditada( numCombinacion, false );  // Es cálculo automático
							}
						}
					}
					// TODO Quedan pendientes "Pot.alt.luces", "Pot3"
					// Las baterías no generan resistencias (no se miden)
				}
				numCombinacion++;
			}
		}

		
	// Gestión de textos

	/** Crea nuevo texto de punto
	 * @param pt	Punto
	 * @return	Texto recién creado
	 */
	private TextoDeCircuito crearNuevoTextoDePunto( Punto pt ) {
		TextoDeCircuito tf = new TextoDeCircuito( 3, pt );
		tf.setBounds( pDibujo.getCoordPant(pt.x) + DIST_X_TEXTO, pDibujo.getCoordPant(pt.y) + DIST_Y_TEXTO, ANCH_TEXTO_INICIAL, ALT_TEXTO, pDibujo.getZoom() );
		return tf;
	}
	
	public boolean textoMuyAlejado( Punto pt ) {
		int distx = Math.abs( pt.x-pt.getTextoAsociado().getLocationAbsoluta().x );
		int disty = Math.abs( pt.y-pt.getTextoAsociado().getLocationAbsoluta().y );
		return (distx>DIST_X_TEXTO*5 || disty>-DIST_Y_TEXTO*5);
	}
	
	// Chequea los textos y los corrige si están muy lejos. Devuelve true si hay cualquier cambio
	private boolean chequeaYCorrigeTextosDeCircuito() {
		boolean cambios = false;
		for (Punto p : lPuntos) {
			if (p.getTextoAsociado()!=null && textoMuyAlejado(p)) {
				p.getTextoAsociado().setLocation( pDibujo.getCoordPant(p.x) + DIST_X_TEXTO, pDibujo.getCoordPant(p.y) + DIST_Y_TEXTO );
				p.getTextoAsociado().setLocationAbsoluta( pDibujo.getCoordPant(p.x) + DIST_X_TEXTO, pDibujo.getCoordPant(p.y) + DIST_Y_TEXTO );
				cambios = true;
			}
		}
		return cambios;
	}


	/** Mueve el texto de circuito a otra posición
	 * @param texto	Texto a mover
	 * @param cX	Distancia X a trasladar
	 * @param cY	Distancia Y a trasladar
	 */
	public void moverTexto( TextoDeCircuito texto, int cX, int cY ) {
		TextoDeCircuito tCopia = (TextoDeCircuito) texto.clone();
		Point pD = texto.getLocation();
		pD.translate( cX, cY );
		texto.setLocation( pD );
		texto.setLocationAbsoluta( pD.x, pD.y );
		if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, texto, tCopia );
	}

	// Gestión de puntos
	
	/** Añade (y devuelve) un punto de conexión nuevo
	 * @param p	Punto de conexión -si ya es un punto se devuelve tal cual, sin crear ninguno nuevo-
	 */
	public Punto addPunto( Point p ) {
		if (p instanceof Punto) return (Punto) p;
		int numNuevoPunto = lPuntos.size(); boolean existe = false; String nom = null;
		do {
			nom = PREFIJO_PUNTO_STD + numNuevoPunto;
			numNuevoPunto++;
			existe = false;
			for (Punto pt : lPuntos) if (pt.equals(nom)) existe = true;  // Si otro punto tiene ese nombre
		} while (existe);  // Mientras ya exista un punto con ese nombre crear otro nombre
		Punto pt = new Punto( p.x, p.y, nom );
		boolean hayCorte = false;
		for (Conexion c : lConexiones) {
			int puntoIni = 0;
			while (c.getAL().size()>puntoIni+1) {
				Punto p1 = c.getAL().get(puntoIni);
				Punto p2 = c.getAL().get(puntoIni+1);
				Punto pCorte = hayPuntoEnLinea( pt, p1.x, p1.y, p2.x, p2.y, 20 );
				if (pCorte!=null) {
					pt = pCorte;
					hayCorte = true;
					c.getAL().add( puntoIni+1, pt );
					// System.out.println( "EN LINEA!!! " + pt + " nueva conexión: " + c.getAL() );  // Para ver si pasa en consola
					break;
				}
				puntoIni++;
			}
			if (hayCorte) break;
		}
		lPuntos.add( pt );
		chequearNombrePuntoCorrecto( pt );
		if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CREAR, pt, null );
		TextoDeCircuito tf = crearNuevoTextoDePunto( pt );
		tf.setText( nom );
		tPuntos.add( tf ); if (pDibujo!=null) pDibujo.add( tf );
		if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CREAR, tf, null );
		return pt;
	}
	
		private Punto hayPuntoEnLinea( Punto p, double p1X, double p1Y, double p2X, double p2Y, int numRec ) {
			double medioX = (p1X + p2X) / 2;
			double medioY = (p1Y + p2Y) / 2;
			if (numRec<=0) {  // Caso base
				double dist = Math.sqrt( Math.pow( (p.x-medioX), 2) + Math.pow( (p.y-medioY), 2 ) );
				if (dist<5) return new Punto( (int)Math.round(medioX), (int)Math.round(medioY), p.getNombre() );
				else return null;
			} else {  // Caso recursivo
				double dist1 = Math.sqrt( Math.pow( (p1X-p.x), 2) + Math.pow( (p1Y-p.y), 2 ) );
				double dist2 = Math.sqrt( Math.pow( (p2X-p.x), 2) + Math.pow( (p2Y-p.y), 2 ) );
				if (dist1<dist2) return hayPuntoEnLinea( p, p1X, p1Y, medioX, medioY, numRec-1 );
				else return hayPuntoEnLinea( p, medioX, medioY, p2X, p2Y, numRec-1 );
			}
		}
	
	/** Crea un nuevo punto si no hay otro suficientemente cerca.
	 * @param p	Punto a consultar
	 * @return	Punto ya creado si está suficientemente cerca, punto nuevo (el mismo recibido) si no había ninguno cerca 
	 */
	public Punto addPuntoSiProcede( Point p ) {
		if (p instanceof Punto) return (Punto)p; // Si ya es punto lo devuelve tal cual
		for (Punto pAnt : lPuntos) {
			if (pAnt.distance( p.getX(), p.getY() ) <= CERCANIA_PUNTOS) {
				return pAnt;
			}
		}
		return addPunto( p );
	}
	
	/** Indica si un punto dado ya existe
	 * @param p	Punto a consultar
	 * @param zoom	Opcional. Si se indica, se considera la cercanía como de pantalla (en pixels), no lógica
	 * @return	Punto ya creado si está suficientemente cerca, el mismo punto recibido -Point- si no había ninguno cerca.
	 * 			Se considera "suficientemente cerca" cuando está a un número de píxels igual o inferior a {@link #CERCANIA_PUNTOS}
	 */
	public Point existePunto( Point p, double... zoom  ) {
		return existePunto( p, CERCANIA_PUNTOS, zoom );
	}
	
	/** Indica si un punto dado ya existe
	 * @param p	Punto a consultar
	 * @param pixelsCercania	Número de píxels de cercanía considerados
	 * @param zoom	Opcional. Si se indica, se considera la cercanía como de pantalla (en pixels), no lógica
	 * @return	Punto ya creado si está suficientemente cerca (pixelsCercania), el mismo punto recibido -Point- si no había ninguno cerca
	 * 			Si hay varios puntos suficientemente cerca, toma el que más cerca esté de todos
	 */
	public Point existePunto( Point p, int pixelsCercania, double... zoom ) {
		if (p instanceof Punto) return p;
		double distMenor = Double.MAX_VALUE;
		Point pRet = p;
		for (Punto pAnt : lPuntos) {
			double dist = pAnt.distance( p.getX(), p.getY() );
			if (zoom.length==0) {
				if (dist <= pixelsCercania && dist < distMenor) {
					pRet = pAnt;
				}
				if (dist <= distMenor) distMenor = dist;
			} else {
				if (dist*zoom[0] <= pixelsCercania && dist*zoom[0] < distMenor) {
					pRet = pAnt;
				}
				if (dist*zoom[0] <= distMenor) distMenor = dist;
			}
		}
		return pRet;
	}
	
	/** Cambia un punto por otro en el circuito
	 * @param ptoSePierde	Punto que se pierde
	 * @param ptoSeQueda	Punto que se queda
	 */
	public void cambiaPunto( Punto ptoSePierde, Punto ptoSeQueda ) {
		lPuntos.remove( ptoSePierde );
		if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, ptoSePierde, null );
		borraTexto( ptoSePierde );
		for (Conector con : getConectores()) {
			Conector conCopia = (Conector) con.clone();
			for (int i=0; i<con.getAL().size(); i++) {
				Punto punto = con.getAL().get(i);
				if (punto==ptoSePierde) {
					con.getAL().set(i, ptoSeQueda);
					if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, con, conCopia );
					break;
				}
			}
		}
		for (Conexion con : getConexiones()) {
			Conexion conCopia = (Conexion) con.clone();
			for (int i=0; i<con.getAL().size(); i++) {
				Punto punto = con.getAL().get(i);
				if (punto==ptoSePierde) {
					con.getAL().set(i, ptoSeQueda);
					if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, con, conCopia );
					break;
				}
			}
		}
		for (Componente com : getComponentes()) {
			Componente comCopia = (Componente) com.clone();
			for (int i=0; i<com.getAL().size(); i++) {
				Punto punto = com.getAL().get(i);
				if (punto==ptoSePierde) {
					com.getAL().set(i, ptoSeQueda);
					if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, com, comCopia );
					break;
				}
			}
		}
	}

	/** Mueve un punto a una coordenada diferente, si esa coordenada no coincide con un punto existente
	 * (si coincide, no hace nada)
	 * @param ptoInicio	Punto a mover
	 * @param pDestino	Coordenada donde moverlo
	 */
	public void moverPunto( Punto ptoInicio, Point pDestino ) {
		if (existePunto(pDestino).equals(pDestino)) {  // No existe ya el punto
			TextoDeCircuito texto = getTexto( ptoInicio );
			if (texto!=null) {  // Mover el texto   (DEBERIA HABERLO)
				moverTexto( texto, pDestino.x-ptoInicio.x, pDestino.y-ptoInicio.y );
			}
			Punto pCopia = (Punto) ptoInicio.clone();
			ptoInicio.setLocation( pDestino.getLocation() );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, ptoInicio, pCopia );
		}
	}
	
	public void removePuntoCo( ObjetoDeGrafico og, int posi ) {
		if (og instanceof Componente) {
			Componente c = (Componente) og;
			Componente cCopia = (Componente) c.clone();
			c.getAL().remove( posi );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, c, cCopia );
		} else if (og instanceof Conexion) {
			Conexion c = (Conexion) og;
			Conexion cCopia = (Conexion) c.clone();
			c.getAL().remove( posi );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, c, cCopia );
		}
	}
	
	public void addPuntoCo( ObjetoDeGrafico og, int posi, Punto p ) {
		if (og instanceof Componente) {
			Componente c = (Componente) og;
			Componente cCopia = (Componente) c.clone();
			c.getAL().add( posi, p );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, c, cCopia );
		} else if (og instanceof Conexion) {
			Conexion c = (Conexion) og;
			Conexion cCopia = (Conexion) c.clone();
			c.getAL().add( posi, p );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, c, cCopia );
		}
	}
	
	public void setPuntoCo( ObjetoDeGrafico og, int posi, Punto p ) {
		if (og instanceof Componente) {
			Componente c = (Componente) og;
			Componente cCopia = (Componente) c.clone();
			c.getAL().set( posi, p );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, c, cCopia );
		} else if (og instanceof Conexion) {
			Conexion c = (Conexion) og;
			Conexion cCopia = (Conexion) c.clone();
			c.getAL().set( posi, p );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, c, cCopia );
		}
	}
	
	public void addPuntoCo( ObjetoDeGrafico og, Punto p ) {
		if (og instanceof Componente) {
			Componente c = (Componente) og;
			Componente cCopia = (Componente) c.clone();
			c.getAL().add( p );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, c, cCopia );
		} else if (og instanceof Conexion) {
			Conexion c = (Conexion) og;
			Conexion cCopia = (Conexion) c.clone();
			c.getAL().add( p );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, c, cCopia );
		}
	}
	
	
	/** Comprueba si un punto pertenece a alguna estructura (conexión, conector, componente)
	 * y devuelve las estructuras a las que pertenece
	 * @param pto	Punto del circuito
	 * @return	Lista de estructuras a las que pertenece el punto, vacía si no pertenece a ninguna
	 */
	public ArrayList<ObjetoDeGrafico> getObjetosDePunto( Punto pto ) {
		ArrayList<ObjetoDeGrafico> listaEstrs = new ArrayList<ObjetoDeGrafico>();
		for (Conector con : getConectores()) {
			if (con.getAL().contains(pto)) listaEstrs.add( con );
		}
		for (Conexion con : getConexiones()) {
			if (con.getAL().contains(pto)) listaEstrs.add( con );
		}
		for (Componente com : getComponentes()) {
			if (com.getAL().contains(pto)) listaEstrs.add( com );
		}
		return listaEstrs;
	}
	
	/** Devuelve la lista de puntos incluidos en el rectángulo indicado
	 * @param pto1	Punto de una esquina del rectángulo
	 * @param pto2	Punto de la otra esquina
	 * @return	Lista de puntos ya creados en el circuito, incluidos en ese rectángulo
	 */
	public ArrayList<Point> buscaPuntosEnZona( Point pto1, Point pto2 ) {
		int xMin = pto1.x; int xMax = pto2.x;
		int yMin = pto1.y; int yMax = pto2.y;
		if (xMin>xMax) { xMin = pto2.x; xMax = pto1.x; }
		if (yMin>yMax) { yMin = pto2.y; yMax = pto1.y; }
		ArrayList<Point> listaPtos = new ArrayList<Point>();
		for (Punto pto : getPuntos()) {
			if (pto.x >= xMin && pto.x <= xMax && pto.y >= yMin && pto.y <= yMax) listaPtos.add( pto );
		}
		return listaPtos;
	}


		private Punto pAnteriorEnRecConexion = null;
	/** Añade una conexión. Si alguno de los puntos no existía (de acuerdo a la distancia establecida), se crea
	 * @param tomarPuntosInteriores	true si la conexión coge todos los puntos que coincidan en la línea, false en caso contrario
	 * @param p1	Punto inicial de conexión
	 * @param p2	Punto final de conexión (y puntos intermedios si proceden)
	 * @return	Conexión recién añadida, null si no se añade ninguna
	 */
	public Conexion addConexion( boolean tomarPuntosInteriores, Point p1, Point... p2 ) {
		if (p2.length>=1) {
			if (tomarPuntosInteriores) {
				Conexion nuevaConexion = new Conexion();
				Punto pt1 = addPuntoSiProcede( p1 );
				nuevaConexion.add( pt1 );
				pAnteriorEnRecConexion = pt1;
				for (Point p : p2) { 
					Punto pt2 = addPuntoSiProcede(p); 
					ArrayList<Punto> intermedios = calcPuntosInteriores( pAnteriorEnRecConexion, pt2 );
					if (!intermedios.isEmpty()) {
						intermedios.sort( new Comparator<Punto>() {
							@Override
							public int compare(Punto o1, Punto o2) {
								return (int) (o1.distance( pAnteriorEnRecConexion ) - o2.distance( pAnteriorEnRecConexion ));
							}
						});
						for (Punto pInt : intermedios) nuevaConexion.add( pInt );
					}
					nuevaConexion.add( pt2 ); 
					pAnteriorEnRecConexion = pt2;
				}
				lConexiones.add( nuevaConexion );
				reposicionaSiHorOVert( nuevaConexion );
				if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CREAR, nuevaConexion, null );
				return nuevaConexion;
			} else {
				Conexion nuevaConexion = new Conexion();
				Punto pt1 = addPuntoSiProcede( p1 );
				nuevaConexion.add( pt1 );
				for (Point p : p2) { Punto pt2 = addPuntoSiProcede(p); nuevaConexion.add( pt2 ); }
				lConexiones.add( nuevaConexion );
				reposicionaSiHorOVert( nuevaConexion );
				if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CREAR, nuevaConexion, null );
				return nuevaConexion;
			}
		} else
			return null;
	}
		private ArrayList<Punto> calcPuntosInteriores( Punto p1, Punto p2 ) {
			ArrayList<Punto> ret = new ArrayList<Punto>();
			for (Punto p : lPuntos) {
				if (p!=p1 && p!=p2) {
					Line2D.Double lin = new Line2D.Double( p1.x, p1.y, p2.x, p2.y );
					double dist = lin.ptSegDist( p );
					if (dist < CERCANIA_INCLUSION_PUNTOS) ret.add( p );
				}
			}
			return ret;
		}
		
	/** Añade un punto a una conexión ya existente.
	 * @param con	Conexión existente
	 * @param pto	Punto nuevo a añadir
	 * @param numPuntoAnterior	Número secuencial de punto de la conexión después del que hay que añadir el nuevo
	 */
	public void addPuntoAConexion( Conexion con, Point pto, int numPuntoAnterior ) {
		if (numPuntoAnterior>=0 && numPuntoAnterior<con.getAL().size()) {
			Punto pt1 = addPuntoSiProcede( pto );
			if (puntoEnQueComponentes( pt1, false ).isEmpty()) pt1.setConectable( false );  // Si está solo lo quita de conectable
			Conexion conAnterior = (Conexion) con.clone();
			con.getAL().add( numPuntoAnterior+1, pt1 );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, con, conAnterior );
		}
	}

	
	/** Añade un conector. Si alguno de los puntos no existía (de acuerdo a la distancia establecida), se crea
	 * @param p1	Punto inicial de conexión
	 * @param p2	Punto final de conexión (y puntos intermedios si proceden)
	 * @return	Conector recién añadido, null si no se añade ninguno
	 */
	public Conexion addConector( Point p1, Point... p2 ) {
		if (p2.length>=1) {
			Conector nuevoConector = new Conector();
			Punto pt1 = addPuntoSiProcede( p1 );
			nuevoConector.add( pt1 );
			for (Point p : p2) { Punto pt2 = addPuntoSiProcede(p); nuevoConector.add( pt2 ); }
			lConectores.add( nuevoConector );
			reposicionaSiHorOVert( nuevoConector );
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CREAR, nuevoConector, null );
			return nuevoConector;
		} else
			return null;
	}

	/** Añade una resistencia. Los dos puntos deben existir ya
	 * @param p1	Punto inicial de resistencia
	 * @param p2	Punto final de resistencia
	 * @return	Resistencia recién añadida, null si no se añade ninguna
	 */
	public Resistencia addResistencia( Punto p1, Punto p2 ) {
		if (p1==null || p2==null) return null;
		Resistencia nuevaRes = new Resistencia();
		nuevaRes.add( p1 );
		nuevaRes.add( p2 );
		lResistencias.add( nuevaRes );
		if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CREAR, nuevaRes, null );
		return nuevaRes;
	}

		
		private JDialog d;
		private boolean confirmar = true;
	/** Añade un componente. Si alguno de los puntos no existía (de acuerdo a la distancia establecida), se crea
	 * @param pedirNombre	Si true, pide el nombre de componente de forma interactiva
	 * @param tipoComp	Tipo de componente que se quiere crear (determina los puntos que tiene, y su funcionamiento)
	 * @param p1	Punto 1 de componente
	 * @param p2	Punto 2 de componente (y puntos adicionales si proceden)
	 * @return	Componente recién añadido, null si no se añade ninguno (no se añade ninguno si se cancela el diálogo o se introduce nombre nulo)
	 */
	public Componente addComponente( boolean pedirNombre, String tipoComp, Point p1, ArrayList<Point> p2 ) {
		if (p2.size()>=1) {
			Componente nuevoComp = Componente.crearComponente( tipoComp );
			if (pedirNombre) {
				confirmar = false;
				JTextField tfEntrada = new JTextField( 20 );
				tfEntrada.addKeyListener( ControladorVentanaEAA.getLast().escTecladoTextos );
				d = new JDialog( VentanaEditorAveriaAuto.getLastVentana(), true );
				d.getContentPane().add( new JLabel(Messages.getString("MarcasAverias.20") + nuevoComp.getTipo()), BorderLayout.NORTH ); //$NON-NLS-1$
				d.getContentPane().add( tfEntrada, BorderLayout.CENTER );
				JPanel pBotones = new JPanel();
				JButton bOk = new JButton( Messages.getString("MarcasAverias.21") ); //$NON-NLS-1$
				JButton bCancel = new JButton( Messages.getString("MarcasAverias.22") ); //$NON-NLS-1$
				tfEntrada.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						confirmar = true;
						d.setVisible( false );
					}
				});
				bOk.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						confirmar = true;
						d.setVisible( false );
					}
				});
				bCancel.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						confirmar = false;
						d.setVisible( false );
					}
				});
				pBotones.add( bOk ); pBotones.add( bCancel );
				d.getContentPane().add( pBotones , BorderLayout.SOUTH );
				d.pack();
				d.setLocationRelativeTo( VentanaEditorAveriaAuto.getLastVentana() );
				d.setVisible( true );
				if (confirmar) {
					if (!tfEntrada.getText().equals("")) { //$NON-NLS-1$
						nuevoComp.setNombre( tfEntrada.getText() );
					} else
						return null;  // Nombre nulo
				} else 
					return null;  // Cancelar
				// Versión con JOptionPane (sin escuchador de Ctrl+Shift = VenantaEditorAveriaAuto.escTecladoTextos)
				// String resp = JOptionPane.showInputDialog( VentanaEditorAveriaAuto.getLastVentana(), nuevoComp.getNombre(), 
				// 		"Introduce nombre de componente " + nuevoComp.getTipo(), JOptionPane.QUESTION_MESSAGE );
				// if (resp!=null && !resp.equals("")) nuevoComp.setNombre( resp );
				// else return null;  // Cancelar o nombre nulo
			}
			if (nuevoComp==null) return null;  // Si el tipo es incorrecto, no crea ningún componente
			if (!tipoComp.equals("Elec") && (p2.size()+1 != nuevoComp.getNumPuntos())) return null;  // Si el número de puntos es incorrecto, no crea ningún componente //$NON-NLS-1$
			Punto pt1 = addPuntoSiProcede( p1 );
			if (pt1.getNombre().startsWith( PREFIJO_PUNTO_STD )) {
				Punto ptCop = (Punto) pt1.clone();
				if (tipoComp.equals("Elec")) { // Especial de electrónica //$NON-NLS-1$
					// pt1.setNombre( nuevoComp.getNombre() + nuevoComp.getSufijoPunto(0) + "0" );
				} else {
					pt1.setNombre( nuevoComp.getNombre() + nuevoComp.getSufijoPunto(0) );
					if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, pt1, ptCop );
					actualizaTexto( pt1 );
				}
			}
			nuevoComp.add( pt1 );
			int numP = 1;
			for (Point p : p2) { 
				Punto pt2 = addPuntoSiProcede(p); nuevoComp.add( pt2 ); 
				if (pt2.getNombre().startsWith( PREFIJO_PUNTO_STD )) {
					Punto ptCop = (Punto) pt2.clone();
					if (tipoComp.equals("Elec")) { // Especial de electrónica //$NON-NLS-1$
						// pt2.setNombre( nuevoComp.getNombre() + nuevoComp.getSufijoPunto(0) + numP );
					} else {
						pt2.setNombre( nuevoComp.getNombre() + nuevoComp.getSufijoPunto(numP) );
						if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, pt2, ptCop );
						actualizaTexto( pt2 );
					}
				}
				numP++;
			}
			aseguraNombreUnicoComponente( nuevoComp );
			lComponentes.add( nuevoComp );
			pDibujo.repaint();
			if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CREAR, nuevoComp, null );
			return nuevoComp;
		} else
			return null;
	}
		/** Asegura que el nombre de un componente sea único. Si no lo es, se cambia con un sufijo (2, 3, 4...) hasta que lo sea.
		 * @param nuevoComp	Componente a asegurar / cambiar nombre
		 */
		public void aseguraNombreUnicoComponente( Componente nuevoComp ) {
			boolean nombreYaExiste = false;
			int sufijo = 1;
			String nombreOrig = ""+nuevoComp.getNombre(); //$NON-NLS-1$
			String nombreNuevo = nombreOrig;
			do {
				nombreYaExiste = false;
				for (Componente c : lComponentes) {  // Compara con todos
					if (c!=nuevoComp) {  // Excepto con él mismo
						if (c.getNombre()!=null && c.getNombre().equals( nombreNuevo )) {
							nombreYaExiste = true;
						}
					}
				}
				if (nombreYaExiste) {
					sufijo++;
					nombreNuevo = nombreOrig + sufijo;
				}
			} while (nombreYaExiste);
			nuevoComp.setNombre( nombreNuevo );
		}

		private void actualizaTexto( ObjetoDeGrafico og ) {
			for( TextoDeCircuito tf : tPuntos ) if (tf.getObjetoAsociado()==og) { 
				TextoDeCircuito tCop = (TextoDeCircuito) tf.clone();
				tf.setText( og.getNombre() ); 
				if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, tf, tCop );
				break; 
			}
		}
	
		private TextoDeCircuito getTexto( ObjetoDeGrafico og ) {
			for( TextoDeCircuito tf : tPuntos ) if (tf.getObjetoAsociado()==og) { return tf; }
			return null;
		}
	
		private void borraTexto( ObjetoDeGrafico og ) {
			for( TextoDeCircuito tf : tPuntos ) 
				if (tf.getObjetoAsociado()==og) {
					tPuntos.remove( tf ); pDibujo.remove( tf );
					if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, tf, null );
					break; 
				}
		}
	
		// Reposiciona los textos de los puntos de la conexión indicada, si está en horizontal o vertical
		private void reposicionaSiHorOVert( Conexion con ) {
			if (con.size()>1) {
				int distX = Math.abs( con.get(0).x - con.get(1).x ); 
				int distY = Math.abs( con.get(0).y - con.get(1).y );
				if (distX==0) {  // Está en vertical
					for (Punto p : con.getAL()) {
						int pos = lPuntos.indexOf( p );
						if (pos!=-1) {
							TextoDeCircuito tf = getTexto( p );
							TextoDeCircuito tCop = (TextoDeCircuito) tf.clone();
							tf.setLocation( pDibujo.getCoordPant(p.x) + DIST_X_TEXTO, pDibujo.getCoordPant(p.y) + DIST_Y_TEXTO );
							tf.setLocationAbsoluta( pDibujo.getCoordPant(p.x) + DIST_X_TEXTO, pDibujo.getCoordPant(p.y) + DIST_Y_TEXTO, pDibujo.getZoom() );
							if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, tf, tCop );
						}
					}
				} else if (distY==0) {  // Está en horizontal
					for (Punto p : con.getAL()) {
						int pos = lPuntos.indexOf( p );
						if (pos!=-1) {
							TextoDeCircuito tf = getTexto( p );
							TextoDeCircuito tCop = (TextoDeCircuito) tf.clone();
							tf.setLocation( pDibujo.getCoordPant(p.x) + DIST_X_TEXTO_VERT, pDibujo.getCoordPant(p.y) + DIST_Y_TEXTO_VERT );
							tf.setLocationAbsoluta( pDibujo.getCoordPant(p.x) + DIST_X_TEXTO_VERT, pDibujo.getCoordPant(p.y) + DIST_Y_TEXTO_VERT, pDibujo.getZoom() );
							if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.CAMBIAR, tf, tCop );
						}
					}
				}
			}
		}

	/** Borra el elemento indicado (si es un punto solo lo borra si no está incluido en ningún componente o conexión)
	 * @param og	Elemento a borrar
	 */
	public void borraElemento( ObjetoDeGrafico og ) {
		ArrayList<ObjetoDeGrafico> txtsBorrados = new ArrayList<ObjetoDeGrafico>();
		if (lConexiones.contains(og)) { lConexiones.remove(og); txtsBorrados.add(og); if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, og, null ); }
		else if (lConectores.contains(og)) { lConectores.remove(og); txtsBorrados.add(og); if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, og, null ); }
		else if (lComponentes.contains(og)) { lComponentes.remove(og); txtsBorrados.add(og); if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, og, null ); }
		else if (lResistencias.contains(og)) { lResistencias.remove(og); if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, og, null ); }
		// Borra puntos (mientras no estén referenciados)
		else if (lPuntos.contains(og)) {  // Borrar punto si nadie lo referencia:
			boolean referenciado = false; 
			for (Conexion c : lConexiones) { if (c.getAL().contains( og )) { referenciado = true; break; } }
			if (!referenciado) for (Conector c : lConectores) { if (c.getAL().contains( og )) { referenciado = true; break; } }
			if (!referenciado) for (Componente c : lComponentes) { if (c.getAL().contains( og )) { referenciado = true; break; } }
			if (!referenciado) for (Resistencia r : lResistencias) { if (r.getAL().contains( og )) { referenciado = true; break; } }
			if (!referenciado) {
				lPuntos.remove(og); txtsBorrados.add(og); if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, og, null );
			}
		}
		// System.out.println( "BORRADO: " + og + " - Textos borrados = " + txtsBorrados );
		for (ObjetoDeGrafico odg : txtsBorrados) borraTexto(odg);
		pDibujo.repaint();
	}
	
		
	/** Devuelve el primer texto del circuito que toca al pixel indicado, null si no lo hay
	 * @param p	Punto de ventana
	 * @return	null si no hay texto en esa coordenada, texto si lo hay
	 */
	public TextoDeCircuito getTextoVisualDe( Point p ) {
		for( TextoDeCircuito tf : tPuntos ) if (tf.getBounds().contains( p )) { return tf; }
		return null;
	}
	
	public void setMarcaPunto( Point p ) { marcaPunto = p; }
	public void setMarcaLinea( Point p ) { marcaLinea = p; marcaRect = null; }
	public void setMarcaRect( Point p ) { marcaRect = p; marcaLinea = null; }
	public void clearMarcas() { marcaPunto = null; marcaLinea = null; marcaRect = null; }
	
	/** Selecciona los elementos que estén dentro del rectángulo indicado
	 * @param esquina1	Esquina inicial de selección
	 * @param esquina2	Esquina final de selección
	 * @param tocando	Si false, se seleccionan los elementos enteramente dentro. Si true, vale con que toquen el rectángulo de selección
	 * @param cFiltro	Clases que filtra los elementos que se seleccionan (si no hay ninguna, se seleccionan todos)
	 */
	public void seleccionarElementos(  boolean anyadirASelExistente, Point esquina1, Point esquina2, boolean tocando, Class<?>... cFiltro ) {
		int minX = Math.min( esquina1.x, esquina2.x );
		int maxX = Math.max( esquina1.x, esquina2.x );
		int minY = Math.min( esquina1.y, esquina2.y );
		int maxY = Math.max( esquina1.y, esquina2.y );
		// System.out.println( "SEL ESQUINA: " + minX + "," + minY + " -> " + maxX + "," + maxY );
		Rectangle2D.Double rect = new Rectangle2D.Double( minX, minY, maxX-minX, maxY-minY );
		if (!anyadirASelExistente) conjObjetosSeleccionados.clear();
		java.util.List<Class<?>> lFiltro = (cFiltro==null) ? new ArrayList<Class<?>>() : Arrays.asList( cFiltro );
		if (lFiltro.isEmpty() || lFiltro.contains(Punto.class))
			for (Punto pto : lPuntos) {
				if (rect.contains(pto)) {
					// System.out.println( "CONTENIDO: " + pto );
					conjObjetosSeleccionados.add( pto );
				}
			}
		if (lFiltro.isEmpty() || lFiltro.contains(Conector.class))
			for (Conector con : lConectores) {
				if (tocando) {  // Selección por toque
					Point ptoAnt = null;
					for (Point pto : con.getAL()) {
						if (ptoAnt!=null) {
							Line2D.Double lin = new Line2D.Double(ptoAnt, pto);
							if (lin.intersects(rect)) {
								conjObjetosSeleccionados.add( con );
								break;
							}
						}
						ptoAnt = pto;
					}
				} else {  // Selección por contenido
					boolean contenido = true;
					for (Point pto : con.getAL()) {
						if (!rect.contains(pto)) {
							contenido = false;
							break;
						}
					}
					if (contenido) conjObjetosSeleccionados.add( con );  // Si contiene a todos los puntos se añade el conector a la selección
				}
			}
		if (lFiltro.isEmpty() || lFiltro.contains(Conexion.class))
			for (Conexion con : lConexiones) {
				if (tocando) {  // Selección por toque
					Point ptoAnt = null;
					for (Point pto : con.getAL()) {
						if (ptoAnt!=null) {
							Line2D.Double lin = new Line2D.Double(ptoAnt, pto);
							if (lin.intersects(rect)) {
								conjObjetosSeleccionados.add( con );
								for (Punto pt : con.getAL()) conjObjetosSeleccionados.add( pt );
								break;
							}
						}
						ptoAnt = pto;
					}
				} else {  // Selección por contenido
					boolean contenido = true;
					for (Point pto : con.getAL()) {
						if (!rect.contains(pto)) {
							contenido = false;
							break;
						}
					}
					if (contenido) conjObjetosSeleccionados.add( con );  // Si contiene a todos los puntos se añade la conexión a la selección
				}
			}
		if (lFiltro.isEmpty() || lFiltro.contains(Resistencia.class))
			for (Resistencia r : lResistencias) {
				if (tocando) {  // Selección por toque
					Point ptoAnt = null;
					for (Point pto : r.getAL()) {
						if (ptoAnt!=null) {
							Line2D.Double lin = new Line2D.Double(ptoAnt, pto);
							if (lin.intersects(rect)) {
								conjObjetosSeleccionados.add( r );
								break;
							}
						}
						ptoAnt = pto;
					}
				} else {  // Selección por contenido
					boolean contenido = true;
					for (Point pto : r.getAL()) {
						if (!rect.contains(pto)) {
							contenido = false;
							break;
						}
					}
					if (contenido) conjObjetosSeleccionados.add( r );  // Si contiene a todos los puntos se añade la resistencia a la selección
				}
			}
		if (lFiltro.isEmpty() || lFiltro.contains(Componente.class))
			for (Componente com : lComponentes) {
				if (tocando) {  // Selección por toque
					Rectangle rectComp = com.getRectangulo(0, 0);
					if (rectComp.intersects(rect)) {
						conjObjetosSeleccionados.add( com );
					}
				} else {  // Selección por contenido
					boolean contenido = true;
					for (Point pto : com.getAL()) {
						if (!rect.contains(pto)) {
							contenido = false;
							break;
						}
					}
					if (contenido) conjObjetosSeleccionados.add( com );  // Si contiene a todos los puntos se añade el componente a la selección
				}
			}
	}

	/** Añade a la selección existente el elemento indicado
	 * @param odg	Elemento a añadir a la selección
	 */
	public void anyadirSeleccionElemento( ObjetoDeGrafico odg ) {
		seleccionarElemento( odg, true );
	}
	
	/** Selecciona el elemento indicado
	 * @param odg	Elemento a seleccionar. Si es null, no se añade
	 * @param anyadirASelExistente	Si true, se añade a selección existente, si false se selecciona solo
	 */
	public void seleccionarElemento( ObjetoDeGrafico odg, boolean anyadirASelExistente ) {
		if (!anyadirASelExistente) conjObjetosSeleccionados.clear();
		if (odg!=null) {
			conjObjetosSeleccionados.add( odg );
			if (odg.getTipoElemento() == TipoElemento.Conexion)
				for (Punto pt : ((Conexion)odg).getAL()) conjObjetosSeleccionados.add( pt );
		}
	}
	
	/** Selecciona cualquier elemento que esté suficientemente cerca del punto indicado
	 * @param p	Punto para la selección, null para borrar la selección
	 * @param cFiltro	Clases que filtra los elementos que se seleccionan (si no hay ninguna, se seleccionan todos)
	 */
	public void seleccionarElementos( boolean anyadirASelExistente, Point p, Class<?>... cFiltro ) {
		// System.out.println( anyadirASelExistente + " - " + p + " - " + cFiltro.length );
		if (!anyadirASelExistente) conjObjetosSeleccionados.clear();
		if (p!=null) {
			cFiltro = null;
			java.util.List<Class<?>> lFiltro = (cFiltro==null) ? new ArrayList<Class<?>>() : Arrays.asList( cFiltro );
			if (lFiltro.isEmpty() || lFiltro.contains(Punto.class))
				for (Punto pto : lPuntos) {
					if (pto.distance( p.x, p.y ) <= CERCANIA_PUNTOS) conjObjetosSeleccionados.add( pto );
				}
			if (lFiltro.isEmpty() || lFiltro.contains(Conector.class))
				for (Conector con : lConectores) {
					Point pIni = con.get(0);
					for (int i=1; i<con.size(); i++) {
						Point pFin = con.get(i);
						Line2D.Float lin = new Line2D.Float( pIni, pFin );
						if (lin.ptSegDist( p ) <= CERCANIA_SELECCION_CONS) {
							conjObjetosSeleccionados.add( con );
							break;
						}
						pIni = pFin;
					}
				}
			if (lFiltro.isEmpty() || lFiltro.contains(Conexion.class))
				for (Conexion con : lConexiones) {
					Point pIni = con.get(0);
					for (int i=1; i<con.size(); i++) {
						Point pFin = con.get(i);
						Line2D.Float lin = new Line2D.Float( pIni, pFin );
						if (lin.ptSegDist( p ) <= CERCANIA_PUNTOS) {
							conjObjetosSeleccionados.add( con );
							for (Punto pt : con.getAL()) conjObjetosSeleccionados.add( pt );
							break;
						}
						pIni = pFin;
					}
				}
			if (lFiltro.isEmpty() || lFiltro.contains(Resistencia.class))
				for (Resistencia r : lResistencias) {
					Line2D.Float lin = new Line2D.Float( r.get(0), r.get(1) );
					if (lin.ptSegDist( p ) <= CERCANIA_PUNTOS) {
						conjObjetosSeleccionados.add( r );
					}
				}
			if (lFiltro.isEmpty() || lFiltro.contains(Componente.class))
				for (Componente com : lComponentes) {
					// Manera antigua: coincidencia con líneas entre vértices
					// for (int i=0; i<com.size(); i++) {
					// 	Point pIni = com.get(i);
					// 	for (int j=i+1; j<com.size(); j++) {
					// 		Point pFin = com.get(j);
					// 		Line2D.Float lin = new Line2D.Float( pIni, pFin );
					// 		if (lin.ptSegDist( p ) <= CERCANIA_PUNTOS) {
					// 			conjObjetosSeleccionados.add( com );
					// 			break;
					// 		}
					// 	}
					// }
					// Manera actual: punto dentro de rectángulo envolvente
					Rectangle r = com.getRectangulo(5,5);
					if (r.contains( p )) conjObjetosSeleccionados.add( com );
				}
		}
	}

	/** Devuelve el número de elementos actualmente seleccionados */
	public int getNumElementosSeleccionados() {
		return conjObjetosSeleccionados.size();
	}
	
	/** Devuelve los elementos actualmente seleccionados
	 * @return	Array de objetos seleccionados
	 */
	public HashSet<ObjetoDeGrafico> getConjElementosSeleccionados() {
		return conjObjetosSeleccionados;
	}
	
	/** Devuelve los elementos actualmente seleccionados
	 * @return	Array de objetos seleccionados
	 */
	public ObjetoDeGrafico[] getElementosSeleccionados() {
		ObjetoDeGrafico[] odgArray = new ObjetoDeGrafico[ conjObjetosSeleccionados.size() ];
		return conjObjetosSeleccionados.toArray( odgArray );
	}
	
		private transient HashSet<ObjetoDeGrafico> conjAntObjsSels = null;
		
	/** Memoriza la selección actual
	 */
	public void memorizaSeleccion() {
		conjAntObjsSels = conjObjetosSeleccionados;
	}
	
	/** Restaura la última selección memorizada  ({@link #memorizaSeleccion()})
	 */
	public void restauraSeleccion() {
		conjObjetosSeleccionados = conjAntObjsSels;
	}
	
	
	/** Devuelve el elemento seleccionado único
	 * @return	Elemento seleccionado. Si hay más de uno o ninguno, devuelve null.
	 */
	public ObjetoDeGrafico getElementoSeleccionado() {
		if (conjObjetosSeleccionados.size()==1) return (ObjetoDeGrafico) (conjObjetosSeleccionados.toArray()[0]);
		return null;
	}
	
	/** Borra todos los elementos actualmente seleccionados */
	public void borraElementosSeleccionados() {
		// System.out.println( conjObjetosSeleccionados );
		// System.out.println( "Puntos = " + lPuntos );
		// System.out.println( "Conexiones = " + lConexiones );
		// System.out.println( "Conectores = " + lConectores );
		// System.out.println( "Componentes = " + lComponentes );
		// System.out.println( "Textos = " + tPuntos );
		// System.out.println( "BORRANDO OBJETOS SELECCIONADOS:");
		ArrayList<ObjetoDeGrafico> txtsBorrados = new ArrayList<ObjetoDeGrafico>();
		for (ObjetoDeGrafico og : conjObjetosSeleccionados) {  // 1. Borra primero los elementos -para dejar puntos sueltos si procede-
			if (lConexiones.contains(og)) { lConexiones.remove(og); txtsBorrados.add(og); if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, og, null ); }
			if (lConectores.contains(og)) { lConectores.remove(og); txtsBorrados.add(og); if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, og, null ); }
			if (lComponentes.contains(og)) { lComponentes.remove(og); txtsBorrados.add(og); if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, og, null ); }
			if (lResistencias.contains(og)) { lResistencias.remove(og); if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, og, null ); }
		}
		for (ObjetoDeGrafico og : conjObjetosSeleccionados) {  // 2. Borra puntos (mientras no estén referenciados)
			if (lPuntos.contains(og)) {  // Borrar punto si nadie lo referencia:
				boolean referenciado = false; 
				for (Conexion c : lConexiones) { if (c.getAL().contains( og )) { referenciado = true; break; } }
				if (!referenciado) for (Conector c : lConectores) { if (c.getAL().contains( og )) { referenciado = true; break; } }
				if (!referenciado) for (Componente c : lComponentes) { if (c.getAL().contains( og )) { referenciado = true; break; } }
				if (!referenciado) for (Resistencia r : lResistencias) { if (r.getAL().contains( og )) { referenciado = true; break; } }
				if (!referenciado) {
					lPuntos.remove(og); txtsBorrados.add(og); if (!deshaciendo) Acciones.getInstance().hacer( Acciones.TipoAccion.BORRAR, og, null );
				}
			}
		}
		// System.out.println( "BORRADO: Textos borrados = " + txtsBorrados );
		for (ObjetoDeGrafico og : txtsBorrados) borraTexto(og);
		conjObjetosSeleccionados.clear();
		pDibujo.repaint();
		// System.out.println( "Puntos = " + lPuntos );
		// System.out.println( "Conexiones = " + lConexiones );
		// System.out.println( "Conectores = " + lConectores );
		// System.out.println( "Componentes = " + lComponentes );
		// System.out.println( "Textos = " + tPuntos );
	}
	
	
	//
	// Hacer y deshacer
	//

	private boolean deshaciendo = false;
	
	/** Deshacer una acción -crear, borrar, cambiar-
	 * @param marcas	Marcas de averías sobre las que se actúa
	 * @param tipo	Crear, borrar o cambiar
	 * @param accion1	acción a deshacer
	 * @param accion2	(null si es crear o borrar)
	 */
	public void deshacer( Acciones.TipoAccion tipo, Accion accion1, Accion accion2 ) {
		deshaciendo = true;
		if (accion1 instanceof Punto) {
			Punto p = (Punto) accion1;
			switch (tipo) {
				case CREAR: {
					lPuntos.remove( p );
					break;
				}
				case BORRAR: {
					lPuntos.add( p );
					chequearNombrePuntoCorrecto( p );	
					break;
				}
				case CAMBIAR: {
					Punto p2 = (Punto) accion2;
					p.x = p2.x;
					p.y = p2.y;
					p.setNombre( p2.getNombre() );
					break;
				}
			}
		} else if (accion1 instanceof Componente) {
			Componente c = (Componente) accion1;
			switch (tipo) {
				case CREAR: {
					lComponentes.remove( c );
					break;
				}
				case BORRAR: {
					lComponentes.add( c );
					break;
				}
				case CAMBIAR: {
					Componente c2 = (Componente) accion2;
					c.getAL().clear(); c.getAL().addAll( c2.getAL() );
					c.setNombre( c2.getNombre() );
					c.setTextoAsociado( c2.getTextoAsociado() );
					c.setTipo( c2.getTipo() );
					c.setNumTipo( c2.getNumTipo() );
					c.setConfig1( c2.getConfig1() );
					c.setConfig2( c2.getConfig2() );
					c.setConfig3( c2.getConfig3() );
					break;
				}
			}
		} else if (accion1 instanceof Conector) {
			Conector c = (Conector) accion1;
			switch (tipo) {
				case CREAR: {
					lConectores.remove( c );
					break;
				}
				case BORRAR: {
					lConectores.add( c );
					break;
				}
				case CAMBIAR: {
					Conector c2 = (Conector) accion2;
					c.getAL().clear(); c.getAL().addAll( c2.getAL() );
					c.setNombre( c2.getNombre() );
					c.setTextoAsociado( c2.getTextoAsociado() );
					break;
				}
			}
		} else if (accion1 instanceof Resistencia) {
			Resistencia r = (Resistencia) accion1;
			switch (tipo) {
				case CREAR: {
					lResistencias.remove( r );
					break;
				}
				case BORRAR: {
					lResistencias.add( r );
					break;
				}
				case CAMBIAR: {
					Resistencia r2 = (Resistencia) accion2;
					r.getAL().clear(); r.getAL().addAll( r2.getAL() );
					r.setNombre( r2.getNombre() );
					break;
				}
			}
		} else if (accion1 instanceof Conexion) {
			Conexion c = (Conexion) accion1;
			switch (tipo) {
				case CREAR: {
					lConexiones.remove( c );
					break;
				}
				case BORRAR: {
					lConexiones.add( c );
					break;
				}
				case CAMBIAR: {
					Conexion c2 = (Conexion) accion2;
					c.getAL().clear(); c.getAL().addAll( c2.getAL() );
					c.setNombre( c2.getNombre() );
					c.setTextoAsociado( c2.getTextoAsociado() );
					break;
				}
			}
		} else if (accion1 instanceof TextoDeCircuito) {
			TextoDeCircuito tc = (TextoDeCircuito) accion1;
			switch (tipo) {
				case CREAR: {
					tPuntos.remove( tc );
					if (pDibujo!=null) pDibujo.remove( tc );
					break;
				}
				case BORRAR: {
					tPuntos.add( tc ); if (pDibujo!=null) pDibujo.add( tc );
					break;
				}
				case CAMBIAR: {
					TextoDeCircuito tc2 = (TextoDeCircuito) accion2;
					tc.setText( tc2.getText() );
					tc.setLocation( tc2.getX(), tc2.getY() );
					break;
				}
			}
		}
		deshaciendo = false;
	}

	// TODO 
	// Cambiar todo menos lo del punto que ya está cambiado
	// Ver cómo se puede hacer bien lo del "CAMBIAR" que necesitará cambiar los datos de accion1 o accion2 (?)
	/** Hacer una acción -crear, borrar, cambiar-
	 * @param marcas	Marcas de averías sobre las que se actúa
	 * @param tipo	Crear, borrar o cambiar
	 * @param accion1	acción a deshacer
	 * @param accion2	(null si es crear o borrar)
	 */
	public void hacer( Acciones.TipoAccion tipo, Accion accion1, Accion accion2 ) {
		deshaciendo = true;
		if (accion1 instanceof Punto) {
			Punto p = (Punto) accion1;
			switch (tipo) {
				case CREAR: {
					lPuntos.add( p );
					chequearNombrePuntoCorrecto( p );	
					break;
				}
				case BORRAR: {
					lPuntos.remove( p );
					break;
				}
				case CAMBIAR: {
					Punto p2 = (Punto) accion2;
					p.x = p2.x;
					p.y = p2.y;
					p.setNombre( p2.getNombre() );
					break;
				}
			}
		} else if (accion1 instanceof Componente) {
			Componente c = (Componente) accion1;
			switch (tipo) {
				case CREAR: {
					lComponentes.remove( c );
					break;
				}
				case BORRAR: {
					lComponentes.add( c );
					break;
				}
				case CAMBIAR: {
					Componente c2 = (Componente) accion2;
					c.getAL().clear(); c.getAL().addAll( c2.getAL() );
					c.setNombre( c2.getNombre() );
					c.setTextoAsociado( c2.getTextoAsociado() );
					c.setTipo( c2.getTipo() );
					c.setNumTipo( c2.getNumTipo() );
					c.setConfig1( c2.getConfig1() );
					c.setConfig2( c2.getConfig2() );
					c.setConfig3( c2.getConfig3() );
					break;
				}
			}
		} else if (accion1 instanceof Conector) {
			Conector c = (Conector) accion1;
			switch (tipo) {
				case CREAR: {
					lConectores.remove( c );
					break;
				}
				case BORRAR: {
					lConectores.add( c );
					break;
				}
				case CAMBIAR: {
					Conector c2 = (Conector) accion2;
					c.getAL().clear(); c.getAL().addAll( c2.getAL() );
					c.setNombre( c2.getNombre() );
					c.setTextoAsociado( c2.getTextoAsociado() );
					break;
				}
			}
		} else if (accion1 instanceof Resistencia) {
			Resistencia r = (Resistencia) accion1;
			switch (tipo) {
				case CREAR: {
					lResistencias.remove( r );
					break;
				}
				case BORRAR: {
					lResistencias.add( r );
					break;
				}
				case CAMBIAR: {
					Resistencia r2 = (Resistencia) accion2;
					r.getAL().clear(); r.getAL().addAll( r2.getAL() );
					r.setNombre( r2.getNombre() );
					break;
				}
			}
		} else if (accion1 instanceof Conexion) {
			Conexion c = (Conexion) accion1;
			switch (tipo) {
				case CREAR: {
					lConexiones.remove( c );
					break;
				}
				case BORRAR: {
					lConexiones.add( c );
					break;
				}
				case CAMBIAR: {
					Conexion c2 = (Conexion) accion2;
					c.getAL().clear(); c.getAL().addAll( c2.getAL() );
					c.setNombre( c2.getNombre() );
					c.setTextoAsociado( c2.getTextoAsociado() );
					break;
				}
			}
		} else if (accion1 instanceof TextoDeCircuito) {
			TextoDeCircuito tc = (TextoDeCircuito) accion1;
			switch (tipo) {
				case CREAR: {
					tPuntos.remove( tc );
					if (pDibujo!=null) pDibujo.remove( tc );
					break;
				}
				case BORRAR: {
					tPuntos.add( tc ); if (pDibujo!=null) pDibujo.add( tc );
					break;
				}
				case CAMBIAR: {
					TextoDeCircuito tc2 = (TextoDeCircuito) accion2;
					tc.setText( tc2.getText() );
					tc.setLocation( tc2.getX(), tc2.getY() );
					break;
				}
			}
		}
		deshaciendo = false;
	}
	
	
	//
	// Dibujado
	//
	
	/** Dibuja todas las marcas en el objeto graphics recibido como parámetro
	 * @param g
	 */
	public void dibuja( Graphics2D g ) {
		if (!mostrarCircuitos) return;
		double zoom = (pDibujo==null)?1.0:pDibujo.getZoom();
		// Conexiones
		for (Conexion lP : lConexiones) {
			lP.dibuja( g, Color.green, STROKE5, zoom  );
		}
		// Conexiones seleccionadas
		for (Conexion lP : lConexiones) {
			if (conjObjetosSeleccionados.contains(lP)) {
				lP.dibuja( g, COLOR_SELECCION, STROKE_SELECCION, zoom );
			}
		}
		// Conectores
		for (Conector lP : lConectores) {
			lP.dibuja( g, Color.magenta, STROKE3, zoom  );
		}
		// Conectores seleccionados
		for (Conector lP : lConectores) {
			if (conjObjetosSeleccionados.contains(lP)) {
				lP.dibuja( g, COLOR_SELECCION, STROKE_SELECCION, zoom  );
			}
		}
		// Componentes
		for (Componente lC : lComponentes) {
			lC.dibuja( g, Color.cyan, STROKE3, zoom );
		}
		// Componentes seleccionados
		for (Componente lC : lComponentes) {
			if (conjObjetosSeleccionados.contains(lC)) {
				lC.dibuja( g, COLOR_SELECCION, STROKE_SELECCION, zoom );
			}
		}
		// Resistencias
		for (Resistencia r : lResistencias) {
			r.dibuja( g, Color.blue, STROKE1, zoom  );
		}
		// Resistencias seleccionadas
		for (Resistencia r : lResistencias) {
			if (conjObjetosSeleccionados.contains(r)) {
				r.dibuja( g, COLOR_SELECCION, STROKE_SELECCION, zoom  );
			}
		}
		// Puntos
		for (Punto p : lPuntos) {
			if (p.isConectable()) {
				p.dibuja( g, Color.blue, STROKE3, zoom  );
			} else {
				p.dibuja( g, Color.green, STROKE1, zoom  );
			}
		}
		// Puntos seleccionados
		for (Punto p : lPuntos) {
			if (conjObjetosSeleccionados.contains(p)) {
				p.dibuja( g, COLOR_SELECCION, STROKE_SELECCION, zoom  );
			}
		}
		// Marcas (si existen)
		g.setStroke( STROKE1 );
		if (marcaPunto!=null) {
			g.setColor( Color.red ); 
			Point pEsc = new Point( pDibujo.getCoordPant(marcaPunto.x), pDibujo.getCoordPant(marcaPunto.y) );
			g.drawOval( pEsc.x-RADIO_MARCA_PUNTO, pEsc.y-RADIO_MARCA_PUNTO, RADIO_MARCA_PUNTO*2, RADIO_MARCA_PUNTO*2 );
			if (marcaLinea!=null) dibujaLinea( g, marcaPunto, marcaLinea, pDibujo.getZoom() );
			if (marcaRect!=null) dibujaRect( g, marcaPunto, marcaRect, pDibujo.getZoom() );
		}
	}
		
	/** Dibuja un punto
	 */
	public static void dibujaPunto( Graphics2D g, Point p, double zoom ) { 
		int pX = (int) Math.round( p.x * zoom );
		int pY = (int) Math.round( p.y * zoom );
		g.drawOval( pX-RADIO_CONEXION, pY-RADIO_CONEXION, 
				RADIO_CONEXION*2, RADIO_CONEXION*2 ); 
	}
	/** Dibuja un óvalo relleno
	 */
	public static void dibujaOvalo( Graphics2D g, Point p, int radio, double zoom ) { 
		int pX = (int) Math.round( p.x * zoom );
		int pY = (int) Math.round( p.y * zoom );
		g.fillOval( pX-radio, pY-radio, radio*2, radio*2 ); 
	}
	/** Dibuja un círculo
	 */
	public static void dibujaCirculo( Graphics2D g, Point p, int radio, double zoom ) { 
		int pX = (int) Math.round( p.x * zoom );
		int pY = (int) Math.round( p.y * zoom );
		g.drawOval( pX-radio, pY-radio, radio*2, radio*2 ); 
	}
	/** Dibuja un círculo relleno
	 */
	public static void dibujaCirculoRelleno( Graphics2D g, Point p, int radio, double zoom ) { 
		int pX = (int) Math.round( p.x * zoom );
		int pY = (int) Math.round( p.y * zoom );
		g.fillOval( pX-radio, pY-radio, radio*2, radio*2 ); 
	}
	/** Dibuja una línea
	 * @param g
	 * @param p1	Punto inicial
	 * @param p2	Punto final
	 * @param zoom	Zoom de dibujado
	 */
	public static void dibujaLinea( Graphics2D g, Point p1, Point p2, double zoom ) {
		g.drawLine( (int)Math.round(p1.x*zoom), (int)Math.round(p1.y*zoom), (int)Math.round(p2.x*zoom), (int)Math.round(p2.y*zoom) ); 
	}
	/** Dibuja un rectángulo
	 * @param g
	 * @param p1	Punto superior izquierdo
	 * @param p2	Punto inferior derecho
	 * @param zoom	Zoom de dibujo
	 */
	public static void dibujaRect( Graphics2D g, Point p1, Point p2, double zoom ) {
		int minX = Math.min( (int)Math.round(p1.x*zoom), (int)Math.round(p2.x*zoom) );
		int maxX = Math.max( (int)Math.round(p1.x*zoom), (int)Math.round(p2.x*zoom) );
		int minY = Math.min( (int)Math.round(p1.y*zoom), (int)Math.round(p2.y*zoom) );
		int maxY = Math.max( (int)Math.round(p1.y*zoom), (int)Math.round(p2.y*zoom) );
		g.drawRect( minX, minY, maxX-minX, maxY-minY ); 
	}
	/** Dibuja un rectángulo con ancho-alto fijo
	 * @param g
	 * @param p1	Punto central del rectángulo
	 * @param ancho	Anchura
	 * @param alto	Altura
	 * @param zoom	Zoom de dibujo
	 */
	public static void dibujaRect( Graphics2D g, Point p1, int ancho, int alto, double zoom ) {
		int pX = (int)Math.round(p1.x*zoom);
		int pY = (int)Math.round(p1.y*zoom);
		g.drawRect( pX-ancho/2, pY-alto/2, ancho, alto ); 
	}
	/** Dibuja una cruz en el medio de dos puntos
	 * @param g
	 * @param p1	Punto inicial
	 * @param p2	Punto final
	 * @param aspa	ancho y alto de aspa de la cruz (en píxels)
	 * @param zoom	Zoom de dibujado
	 */
	public static void dibujaCruz( Graphics2D g, Point p1, Point p2, int aspa, double zoom ) {
		int p1X = (int)Math.round(p1.x*zoom);
		int p1Y = (int)Math.round(p1.y*zoom);
		int p2X = (int)Math.round(p2.x*zoom);
		int p2Y = (int)Math.round(p2.y*zoom);
		int centroX = (p1X+p2X)/2;
		int centroY = (p1Y+p2Y)/2;
		g.drawLine( centroX-aspa, centroY-aspa, centroX+aspa, centroY+aspa);
		g.drawLine( centroX-aspa, centroY+aspa, centroX+aspa, centroY-aspa);
	}

	/** Dibuja un rectángulo
	 * @param pIni	Punto con coordenadas absolutas
	 * @param zoom	Zoom en % (1.0 para 100%)
	 * @return	Punto relativo al zoom indicado
	 */
	public static Point aplicaZoom( Point pIni, double zoom ) {
		int pX = (int)Math.round(pIni.x*zoom);
		int pY = (int)Math.round(pIni.y*zoom);
		return new Point(pX,pY);
	}

		
		private String path;  // Path de fichero
		public String getPath() { return path; }
		
		private boolean guardar = false;  // Info de si se guarda
	/** Pide interactivamente un nombre de fichero y guarda los datos principales de dibujo de circuito en él
	 * @return	null si no se ha guardado, o el path absoluto del fichero si se ha guardado
	 */
	public String guardar() {
		guardar = true;
		File fPath = pedirFicheroDatos( Messages.getString("MarcasAverias.28") ); if (fPath==null) return null; //$NON-NLS-1$
		path = fPath.getAbsolutePath();
		if (!path.toUpperCase().endsWith("DAT") && !path.toUpperCase().endsWith("AUTOG")) { //$NON-NLS-1$ //$NON-NLS-2$
			path = path + ".autoG"; //$NON-NLS-1$
			fPath = new File(path);
		}
		if (fPath.exists()) {  // Pide confirmación de sobreescritura
			int conf = JOptionPane.showConfirmDialog( null, 
					Messages.getString("MarcasAverias.32"),  //$NON-NLS-1$
					Messages.getString("MarcasAverias.33"), JOptionPane.YES_NO_OPTION ); //$NON-NLS-1$
			if (conf!=0) return null;  // si no hay confirmación no seguimos
		}
		guardar( path );
		guardar = false;
		if (!lResistencias.isEmpty()) {  // Si ya está en modo de voltaje/resistencia
			VentanaEditorAveriaAuto v = VentanaEditorAveriaAuto.getLastVentana();
			// Comprobar si hay datos de configuración vacíos y avisar en ese caso.
			if (v.tfConfigTiempoSegs.getText().equals("") || v.tfConfigPuntuacionPartida.getText().equals("") || v.tfConfigNumMinMedidasResistencia.getText().equals("") || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				v.tfConfigNumMaxMedidasResistencia.getText().equals("") || v.tfConfigNumMinMedidasVoltaje.getText().equals("") || v.tfConfigNumMaxMedidasVoltaje.getText().equals("")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				JOptionPane.showMessageDialog( null, Messages.getString("MarcasAverias.40") + //$NON-NLS-1$
						Messages.getString("MarcasAverias.41"), //$NON-NLS-1$
						Messages.getString("MarcasAverias.42"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$
		}
		return path;
	}
	/** Guarda los datos principales de dibujo de circuito enel fichero indicado, sobreescribiéndolo si existe
	 */
	public void guardar( String path ) {
		// Salva datos al fichero
		if (path.toUpperCase().endsWith("DAT")) {  // Formato binario //$NON-NLS-1$
			try {
				ObjectOutputStream oos = new ObjectOutputStream( new FileOutputStream(path) );
				oos.writeObject( nomFicheroCircuito );
				oos.writeObject( modoEdicion );
				oos.writeObject( modoResistencias );
				oos.writeObject( combsInterrupcion );
				oos.writeObject( lPuntos );
				oos.writeObject( tPuntos );
				oos.writeObject( lConexiones );
				oos.writeObject( lConectores );
				oos.writeObject( lComponentes );
				oos.writeObject( lResistencias );
				oos.close();
				// JOptionPane.showMessageDialog( null, "El fichero " + 
				//		path + " se ha guardado con los datos.", "Salvado correcto", JOptionPane.INFORMATION_MESSAGE );
			} catch (Exception e2) {
				e2.printStackTrace();
				JOptionPane.showMessageDialog( null, Messages.getString("MarcasAverias.44") +  //$NON-NLS-1$
						path + Messages.getString("MarcasAverias.45"), Messages.getString("MarcasAverias.46"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {  // Formato de texto
			escribirAFicheroTexto( path );
		}
		logger.log( Level.INFO, Messages.getString("MarcasAverias.47") + path ); //$NON-NLS-1$
	}
	
		// Escribir en formato de texto
		private void escribirAFicheroTexto( String path ) {
			try {
				PrintStream ps = new PrintStream( new FileOutputStream( path ) );
				ps.println( "[NOMBRE-FICHERO-CIRCUITO]" );  //$NON-NLS-1$
					ps.println( nomFicheroCircuito );
				ps.println( "[CONFIG]" );  // Versión 0.23 o posterior, con más configuración  // Versión 0.12 o posterior, con configuración //$NON-NLS-1$
					ps.println( VentanaEditorAveriaAuto.getLastVentana().tfConfigAncho.getText() + "," +  //$NON-NLS-1$
						VentanaEditorAveriaAuto.getLastVentana().tfConfigAlto.getText() + "," +  //$NON-NLS-1$
						VentanaEditorAveriaAuto.getLastVentana().tfConfigTiempoSegs.getText() + "," +  //$NON-NLS-1$
						VentanaEditorAveriaAuto.getLastVentana().tfConfigPuntuacionPartida.getText() + "," +  //$NON-NLS-1$
						VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMinMedidasVoltaje.getText() + "," +  //$NON-NLS-1$
						VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMaxMedidasVoltaje.getText() + "," +  //$NON-NLS-1$
						VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMinMedidasResistencia.getText() + "," +  //$NON-NLS-1$
						VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMaxMedidasResistencia.getText() );
				ps.println( "[MODO-EDICION]" );  //$NON-NLS-1$
					ps.println( modoEdicion );
				ps.println( "[MODO-RESISTENCIAS]" );  //$NON-NLS-1$
					ps.println( modoResistencias );
				ps.println( "[LISTA-PUNTOS]" ); //$NON-NLS-1$
					for (Punto p : lPuntos) ps.println( " " + p.aTexto() ); //$NON-NLS-1$
				ps.println( "[LISTA-CONEXIONES]" );  //$NON-NLS-1$
					for (Conexion c : lConexiones) ps.println( " " + c.aTexto() ); //$NON-NLS-1$
				ps.println( "[LISTA-CONECTORES]" );  //$NON-NLS-1$
					for (Conector c : lConectores) ps.println( " " + c.aTexto() ); //$NON-NLS-1$
				ps.println( "[LISTA-COMPONENTES]" );  //$NON-NLS-1$
					for (Componente c : lComponentes) ps.println( " " + c.aTexto() ); //$NON-NLS-1$
				ps.println( "[LISTA-RESISTENCIAS]" );  //$NON-NLS-1$
					for (Resistencia r : lResistencias) ps.println( " " + r.aTexto() ); //$NON-NLS-1$
				ps.println( "[COMBINACIONES-INTERRUPCION]" );  //$NON-NLS-1$
					for (CombinacionInterrupcion ci : combsInterrupcion) ps.println( " " + ci.toString() ); //$NON-NLS-1$
				ps.println( "[LISTA-TEXTOS]" );  //$NON-NLS-1$
					for (TextoDeCircuito t : tPuntos) ps.println( " " + t.aTexto() ); //$NON-NLS-1$
				ps.println( "[AYUDA]" );  // Versión 0.23 o posterior //$NON-NLS-1$
					String ayudas = VentanaEditorAveriaAuto.getLastVentana().taConfigURLAyuda.getText();
					StringTokenizer st = new StringTokenizer( ayudas, "\n" ); //$NON-NLS-1$
					while (st.hasMoreTokens()) {
						String lineaAyuda = st.nextToken();
						ps.println( lineaAyuda );
					}
				ps.println( "[AYUDA-COMPONENTES]" ); //$NON-NLS-1$
					ayudas = VentanaEditorAveriaAuto.getLastVentana().taConfigURLElementos.getText();
					st = new StringTokenizer( ayudas, "\n" ); //$NON-NLS-1$
					while (st.hasMoreTokens()) {
						String lineaAyuda = st.nextToken();
						ps.println( lineaAyuda );
					}
//				ps.println( "[DESCRIPCION]" );
//					ayudas = VentanaEditorAveriaAuto.getLastVentana().taConfigDescripcion.getText();
//					st = new StringTokenizer( ayudas, "\n" );
//					while (st.hasMoreTokens()) {
//						String lineaDesc = st.nextToken();
//						ps.println( lineaDesc );
//					}
				ps.close();
				// JOptionPane.showMessageDialog( null, "El fichero " + 
				//		path + " se ha guardado con los datos.", "Salvado correcto", JOptionPane.INFORMATION_MESSAGE );
				// Guardar fichero gráfico
				String ultGrafico = pDibujo.getUltimoFicheroGraficoCargado();
				if (ultGrafico!=null) {
					int posExt = ultGrafico.lastIndexOf("."); //$NON-NLS-1$
					int posExtEnPath = path.lastIndexOf("."); //$NON-NLS-1$
					if (posExt>0 && posExtEnPath>0) {
						String extension = ultGrafico.substring( posExt+1 );
						String pathGrafico = path.substring(0,posExtEnPath) + "." + extension; //$NON-NLS-1$
						if (new File(ultGrafico).exists())
							Files.copy( Paths.get( ultGrafico ), Paths.get( pathGrafico ), StandardCopyOption.REPLACE_EXISTING );
					}
				}
			} catch (Exception e2) {
				e2.printStackTrace();
				JOptionPane.showMessageDialog( null, Messages.getString("MarcasAverias.80") +  //$NON-NLS-1$
						path + Messages.getString("MarcasAverias.81"), Messages.getString("MarcasAverias.82"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	
		// Pide interactivamente un fichero existente de datos
		// (null si no se selecciona)
		private File pedirFicheroDatos( String mens ) {
			File dirActual = new File( ControladorVentanaEAA.getUltimoDirectorio() );
			JFileChooser chooser = new JFileChooser( dirActual );
			chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
			chooser.setApproveButtonText( Messages.getString("MarcasAverias.83") ); //$NON-NLS-1$
			chooser.setFileFilter( new FileNameExtensionFilter( 
					Messages.getString("MarcasAverias.1"), "autoG", "dat" ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			chooser.setDialogTitle( mens );
			if (guardar) {
				String ultimoFich = ControladorVentanaEAA.getProperty( "ULTIMO-FIC" ); //$NON-NLS-1$
				if (ultimoFich!=null && !ultimoFich.equals("")) { //$NON-NLS-1$
					Path p = Paths.get(ultimoFich);
					if (p!=null)
						chooser.setSelectedFile( p.toFile() );
				}
			}
			int returnVal = chooser.showOpenDialog( null );
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				ControladorVentanaEAA.actualizarUltimoDir( chooser.getSelectedFile() );
				return chooser.getSelectedFile();
			} else 
				return null;
		}

		
		/** Borra todos los elementos de este objeto - no se puede deshacer */
		public void borraTodo() {
			lConexiones.clear();
			lConectores.clear();
			lComponentes.clear();
			lResistencias.clear();
			lPuntos.clear();
			for( TextoDeCircuito tf : tPuntos ) { pDibujo.remove( tf ); }
			tPuntos.clear();
			Acciones.getInstance().reset( this );   // Reinicia las acciones
		}

		/** Reinicia un sistema en blanco
		 */
		public void reiniciar() {
			nomFicheroCircuito = ""; //$NON-NLS-1$
			modoEdicion = true;
			modoResistencias = false;
			lPuntos = new ArrayList<Punto>();
			lConexiones = new ArrayList<Conexion>();
			lConectores = new ArrayList<Conector>();
			lComponentes = new ArrayList<Componente>();
			lResistencias = new ArrayList<Resistencia>();
			tPuntos = new ArrayList<TextoDeCircuito>();
			combsInterrupcion = new ArrayList<CombinacionInterrupcion>();
			combsInterrupcionArray = null;
			ControladorVentanaEAA.getLast().setModoEdicion( modoEdicion );
			ControladorVentanaEAA.getLast().setModoResistencias( modoResistencias );
			if (pDibujo!=null) {
				pDibujo.removeAll();
				pDibujo.setFondoImagen( nomFicheroCircuito );
				setPanelDibujo(pDibujo);
				TextoDeCircuito.reloadPanel( pDibujo );
			}
			JOptionPane.showMessageDialog( null, Messages.getString("MarcasAverias.90")  //$NON-NLS-1$
					, Messages.getString("MarcasAverias.91"), JOptionPane.INFORMATION_MESSAGE ); //$NON-NLS-1$
			Acciones.getInstance().reset( this );   // Reinicia las acciones
			logger.log( Level.INFO, Messages.getString("MarcasAverias.92") ); //$NON-NLS-1$
		}

		
		/** Carga un fichero de datos de circuito existente
		 */
		@SuppressWarnings("unchecked")
		public void cargar( String nomFic ) {
			if (nomFic==null) return;
			File fPath = new File(nomFic);
			if (!fPath.exists()) return;
			path = nomFic;
			ControladorVentanaEAA.getLast().reiniciarConfiguracion();
			if (fPath.exists() && path.toUpperCase().endsWith(".DAT")) {  //$NON-NLS-1$
				// Carga datos del fichero
				try {
					ObjectInputStream ois = new ObjectInputStream( new FileInputStream(path) );
					nomFicheroCircuito = (String) ois.readObject();
					modoEdicion = (Boolean) ois.readObject();
					modoResistencias = (Boolean) ois.readObject();
					combsInterrupcion = (ArrayList<CombinacionInterrupcion>) ois.readObject();
					lPuntos = (ArrayList<Punto>) ois.readObject();
					tPuntos = (ArrayList<TextoDeCircuito>) ois.readObject();
						for (TextoDeCircuito tc : tPuntos) tc.reloadListener();  // Recarga los escuchadores (no se serializan)
					lConexiones = (ArrayList<Conexion>) ois.readObject();
					lConectores = (ArrayList<Conector>) ois.readObject();
					lComponentes = (ArrayList<Componente>) ois.readObject();
					lResistencias = (ArrayList<Resistencia>) ois.readObject();
					ois.close();
					if (pDibujo!=null) {
						pDibujo.removeAll();
						pDibujo.setFondoImagen( nomFicheroCircuito );
						setPanelDibujo(pDibujo);
						TextoDeCircuito.reloadPanel( pDibujo );
					}
					ControladorVentanaEAA.getLast().setModoEdicion( modoEdicion );
					ControladorVentanaEAA.getLast().setModoResistencias( modoResistencias );
					JOptionPane.showMessageDialog( null, Messages.getString("MarcasAverias.94") +  //$NON-NLS-1$
							path + Messages.getString("MarcasAverias.95"), Messages.getString("MarcasAverias.96"), JOptionPane.INFORMATION_MESSAGE ); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (Exception e2) {
					e2.printStackTrace();
					JOptionPane.showMessageDialog( null, Messages.getString("MarcasAverias.97") +  //$NON-NLS-1$
							path + Messages.getString("MarcasAverias.98"), Messages.getString("MarcasAverias.99"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} else if (fPath.exists() && path.toUpperCase().endsWith(".AUTOG")) {  //$NON-NLS-1$
				leerDeFicheroTexto( path );
			}
			if (chequeaYCorrigeTextosDeCircuito()) {
				JOptionPane.showMessageDialog( null, Messages.getString("MarcasAverias.101"), Messages.getString("MarcasAverias.102"), JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$ //$NON-NLS-2$
				logger.log( Level.INFO, Messages.getString("MarcasAverias.103") + nomFic ); //$NON-NLS-1$
			}
			logger.log( Level.INFO, Messages.getString("MarcasAverias.104") + nomFic ); //$NON-NLS-1$
		}

		/** Pide interactivamente un nombre de fichero y carga los datos principales de dibujo de circuito
		 * @return	true si se carga un fichero, false si no se carga ninguno.
		 */
		public boolean cargar() {
			File fPath = pedirFicheroDatos( Messages.getString("MarcasAverias.105") ); if (fPath==null) return false; //$NON-NLS-1$
			path = fPath.getAbsolutePath();
			cargar( path );
			return true;
		}

			// Lee de fichero en formato texto
			@SuppressWarnings("resource")
			private void leerDeFicheroTexto( String path ) {
				String linea = ""; //$NON-NLS-1$
				BufferedReader br = null;
				try {
					br = new BufferedReader( new InputStreamReader( new FileInputStream(path) ));
					linea = br.readLine();
					if (!linea.equals( "[NOMBRE-FICHERO-CIRCUITO]" )) throw new NullPointerException("[NOMBRE-FICHERO-CIRCUITO]"); // Error //$NON-NLS-1$ //$NON-NLS-2$
					nomFicheroCircuito = br.readLine();
					linea = br.readLine();
					if (linea.equals( "[CONFIG]" )) {  // Versión 0.12 o posterior, con configuración //$NON-NLS-1$
						String linea1 = br.readLine();
						if (linea1.contains(",")) {  // Versión 0.23 o posterior, con configuración en una línea //$NON-NLS-1$
							linea1 = linea1.replaceAll( ",,", ", ," );  // Mete espacios para el tokenizer por si hay valores vacíos //$NON-NLS-1$ //$NON-NLS-2$
							linea1 = " " + linea1.replaceAll( ",,", ", ," ) + " "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							StringTokenizer st = new StringTokenizer(linea1,","); //$NON-NLS-1$
							VentanaEditorAveriaAuto.getLastVentana().tfConfigAncho.setText( st.nextToken().trim() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigAlto.setText( st.nextToken().trim() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigTiempoSegs.setText( st.nextToken().trim() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigPuntuacionPartida.setText( st.nextToken().trim() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMinMedidasVoltaje.setText( st.nextToken().trim() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMaxMedidasVoltaje.setText( st.nextToken().trim() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMinMedidasResistencia.setText( st.nextToken().trim() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMaxMedidasResistencia.setText( st.nextToken().trim() );
						} else {  // Versión entre 0.12 y 0.22, con configuración multilínea
							VentanaEditorAveriaAuto.getLastVentana().tfConfigAncho.setText( linea1 );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigAlto.setText( br.readLine() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigTiempoSegs.setText( br.readLine() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigPuntuacionPartida.setText( br.readLine() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMinMedidasVoltaje.setText( br.readLine() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMaxMedidasVoltaje.setText( br.readLine() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMinMedidasResistencia.setText( br.readLine() );
							VentanaEditorAveriaAuto.getLastVentana().tfConfigNumMaxMedidasResistencia.setText( br.readLine() );
							VentanaEditorAveriaAuto.getLastVentana().taConfigURLAyuda.setText( br.readLine() );
						}
						linea = br.readLine();
					} // else Versión 1.12 o anterior, sin configuración
					if (linea.equals( "[MODO-EDICION]" )) {  // Versión 1.04 o posterior, con modo edición //$NON-NLS-1$
						modoEdicion = true;
						String modo = br.readLine();
						if (modo.equalsIgnoreCase("false")) modoEdicion = false; //$NON-NLS-1$
						linea = br.readLine();
					} else { // Versión 1.03 o anterior, sin modo edición
						modoEdicion = true;
					}
					if (linea.equals( "[MODO-RESISTENCIAS]" )) {  // Versión 1.11 o posterior, con modo resistencias //$NON-NLS-1$
						modoResistencias = false;
						String modo = br.readLine();
						if (modo.equalsIgnoreCase("true")) modoResistencias = true; //$NON-NLS-1$
						linea = br.readLine();
					} else { // Versión 1.10 o anterior, sin modo resistencias
						modoResistencias = false;
					}
					if (!linea.equals( "[LISTA-PUNTOS]" )) throw new NullPointerException("[LISTA-PUNTOS] " + linea ); // Error //$NON-NLS-1$ //$NON-NLS-2$
						linea = br.readLine();
						lPuntos = new ArrayList<Punto>();
						while (linea != null && !linea.startsWith("[")) { // Proceso de línea //$NON-NLS-1$
							if (!linea.equals("")) { //$NON-NLS-1$
								Punto p = new Punto( linea );
								lPuntos.add( p );
								chequearNombrePuntoCorrecto( p );	
							}
							linea = br.readLine();
						}
					if (!linea.equals( "[LISTA-CONEXIONES]" )) throw new NullPointerException("[LISTA-CONEXIONES] " + linea ); // Error //$NON-NLS-1$ //$NON-NLS-2$
						linea = br.readLine();
						lConexiones = new ArrayList<Conexion>();
						while (linea != null && !linea.startsWith("[")) { // Proceso de línea //$NON-NLS-1$
							if (!linea.equals("")) { //$NON-NLS-1$
								Conexion c = Conexion.crearConexion( linea, lPuntos );
								lConexiones.add( c );
							}
							linea = br.readLine();
						}
					if (!linea.equals( "[LISTA-CONECTORES]" )) throw new NullPointerException("[LISTA-CONECTORES] " + linea ); // Error //$NON-NLS-1$ //$NON-NLS-2$
						linea = br.readLine();
						lConectores = new ArrayList<Conector>();
						while (linea != null && !linea.startsWith("[")) { // Proceso de línea //$NON-NLS-1$
							if (!linea.equals("")) { //$NON-NLS-1$
								Conector c = Conector.crearConector( linea, lPuntos );
								lConectores.add( c );
							}
							linea = br.readLine();
						}
					if (!linea.equals( "[LISTA-COMPONENTES]" )) throw new NullPointerException("[LISTA-COMPONENTES] " + linea ); // Error //$NON-NLS-1$ //$NON-NLS-2$
						linea = br.readLine();
						lComponentes = new ArrayList<Componente>();
						while (linea != null && !linea.startsWith("[")) { // Proceso de línea //$NON-NLS-1$
							if (!linea.equals("")) { //$NON-NLS-1$
								Componente c = Componente.crearComponente( linea, lPuntos );
								aseguraNombreUnicoComponente( c );
								lComponentes.add( c );
							}
							linea = br.readLine();
						}
					lResistencias = new ArrayList<Resistencia>();
					if (linea.equals( "[LISTA-RESISTENCIAS]" )) { // Versión 1.11 en adelante //$NON-NLS-1$
						linea = br.readLine();
						lResistencias = new ArrayList<Resistencia>();
						while (linea != null && !linea.startsWith("[")) { // Proceso de línea //$NON-NLS-1$
							if (!linea.equals("")) { //$NON-NLS-1$
								Resistencia r = Resistencia.crearResistencia( linea, lPuntos );
								lResistencias.add( r );
							}
							linea = br.readLine();
						}
					}
					combsInterrupcion = new ArrayList<CombinacionInterrupcion>();
					combsInterrupcionArray = null;
					if (linea.equals( "[COMBINACIONES-INTERRUPCION]" )) {  // Versión 1.04 o posterior, con modo edición //$NON-NLS-1$
						linea = br.readLine();
						while (linea != null && !linea.startsWith("[")) { // Proceso de línea //$NON-NLS-1$
							if (!linea.equals("")) //$NON-NLS-1$
								addCombInterrupcion( linea );
							linea = br.readLine();
						}
					}
					if (!linea.equals( "[LISTA-TEXTOS]" )) throw new NullPointerException("[LISTA-TEXTOS] "); // Error //$NON-NLS-1$ //$NON-NLS-2$
						linea = br.readLine();
						tPuntos = new ArrayList<TextoDeCircuito>();
						while (linea != null && !linea.startsWith("[")) { // Proceso de línea //$NON-NLS-1$
							if (!linea.equals("")) { //$NON-NLS-1$
								TextoDeCircuito tc = TextoDeCircuito.crearTextoDeCircuito( linea, lPuntos, lConexiones, lConectores, lComponentes, pDibujo.getZoom() );
								tc.setEditable( true );
								tPuntos.add( tc );
							}
							linea = br.readLine();
						}
					if (linea!=null && linea.equals( "[AYUDA]" )) {  // Versión 1.23 o posterior, con información de ayuda multilingüe //$NON-NLS-1$
						linea = br.readLine();
						String ayuda = ""; //$NON-NLS-1$
						while (linea != null && !linea.startsWith("[")) { // Proceso de línea //$NON-NLS-1$
							ayuda += linea;
							linea = br.readLine();
							if (linea!=null) ayuda += "\n"; //$NON-NLS-1$
						}
						VentanaEditorAveriaAuto.getLastVentana().taConfigURLAyuda.setText( ayuda );
					}
					if (linea!=null && linea.equals( "[AYUDA-COMPONENTES]" )) {  // Versión 1.23 o posterior, con información de ayuda de componentes multilingüe //$NON-NLS-1$
						linea = br.readLine();
						String ayuda = ""; //$NON-NLS-1$
						while (linea != null && !linea.startsWith("[")) { // Proceso de línea //$NON-NLS-1$
							ayuda += linea;
							linea = br.readLine();
							if (linea!=null) ayuda += "\n"; //$NON-NLS-1$
						}
						VentanaEditorAveriaAuto.getLastVentana().taConfigURLElementos.setText( ayuda );
					}
//					if (linea!=null && linea.equals( "[DESCRIPCION]" )) {  // Versión 1.23 o posterior, con información de descripción de la actividad
//						linea = br.readLine();
//						String descripcion = "";
//						while (linea != null && !linea.startsWith("[")) { // Proceso de línea
//							descripcion += linea;
//							linea = br.readLine();
//							if (linea!=null) descripcion += "\n";
//						}
//						VentanaEditorAveriaAuto.getLastVentana().taConfigDescripcion.setText( descripcion );
//					}
					ControladorVentanaEAA.getLast().setModoEdicion( modoEdicion );
					ControladorVentanaEAA.getLast().setModoResistencias( modoResistencias );
					if (pDibujo!=null) {
						pDibujo.removeAll();
						if (new File(nomFicheroCircuito).exists())
							pDibujo.setFondoImagen( nomFicheroCircuito );
						else {
							int posExt = path.lastIndexOf("."); //$NON-NLS-1$
							int posExtEnOrig = nomFicheroCircuito.lastIndexOf("."); //$NON-NLS-1$
							if (posExt>0 && posExtEnOrig>0) {
								String extension = nomFicheroCircuito.substring( posExtEnOrig+1 );
								String pathGrafico = path.substring(0,posExt) + "." + extension; //$NON-NLS-1$
								if (new File(pathGrafico).exists())
									pDibujo.setFondoImagen( pathGrafico );
							}
						}
						setPanelDibujo(pDibujo);
						TextoDeCircuito.reloadPanel( pDibujo );
					}
					// JOptionPane.showMessageDialog( null, "El fichero " + 
					// 		path + " se ha cargado con los datos.", "Cargado correcto", JOptionPane.INFORMATION_MESSAGE );
					br.close();
				} catch (Exception e2) {
					e2.printStackTrace();
					JOptionPane.showMessageDialog( null, Messages.getString("MarcasAverias.159") +  //$NON-NLS-1$
							path + Messages.getString("MarcasAverias.160"), Messages.getString("MarcasAverias.161") + linea, JOptionPane.ERROR_MESSAGE ); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		
		/** Calcula y visualiza los cortes entre conexiones
		 */
		public void calcCortes() {
			for (int i=0; i<lConexiones.size()-1; i++) {
				Conexion seg = lConexiones.get(i);
				for (int j=i+1; j<lConexiones.size(); j++) {
					Conexion seg2 = lConexiones.get(j);
					Line2D.Double lin = new Line2D.Double( seg.get(0).x, seg.get(0).y, seg.get(1).x, seg.get(1).y );
					calcCortesRec( lin, seg2.get(0).x, seg2.get(0).y, seg2.get(1).x, seg2.get(1).y, 20 );
				}
			}
		}
		
		// Método recursivo
		// Recibe una línea y un segmento de dos puntos y recursivamente va aproximando la mitad de ese segmento,
		// en función de cuál de los dos extremos esté más cerca de la línea
		// Usar el método ptSegDist( x, y ) de la clase Line2D.Double
		// Entender que si esta distancia después de 20 llamadas recursivas es menor que 0.0001, es que hay un punto de corte.
		// Visualizarlo solo en ese caso.
		private void calcCortesRec( Line2D.Double lin, double x1, double y1, double x2, double y2, int numLlams ) {
			double xMed = (x1+x2)/2;
			double yMed = (y1+y2)/2;
			// System.out.println( "temp " + xMed + "," + yMed + " -> " + lin.ptSegDist(xMed,yMed) + "     " + numLlams );
			if (numLlams<0) {  // Caso base
				if (lin.ptSegDist(xMed, yMed)<0.0001) {
					// System.out.println( "Punto de corte: (" + xMed + "," + yMed + ")" );  // A consola
					int xM = (int) Math.round( xMed ); int yM = (int) (Math.round(yMed));
					pDibujo.getGraphics().drawOval( (xM-10), (yM-10), 20, 20 );  // Dibujo a pantalla
					pDibujo.getGraphics().drawOval( (xM-2), (yM-2), 4, 4 );
				}
			} else {  // Caso recursivo
				double dist1 = lin.ptSegDist( x1, y1 );
				double dist2 = lin.ptSegDist( x2, y2 );
				if (dist1<dist2)
					calcCortesRec( lin, x1, y1, xMed, yMed, numLlams-1 );
				else
					calcCortesRec( lin, xMed, yMed, x2, y2, numLlams-1 );
			}
		}

		/** Calcula si un punto está en la línea de alguna conexión
		 * @param p
		 * @return	null si no está, un array con la conexión y el número de punto de la conexión inicial del segmento al que está cercano ese punto
		 */
		public Object[] enConexion( Point p ) {
			for (int i=0; i<lConexiones.size()-1; i++) {
				Conexion con = lConexiones.get(i);
				if (con.getAL().size()>0) {
					Punto pAnt = con.getAL().get(0);
					for (int j=1; j<con.getAL().size(); j++) {
						Punto pAct = con.getAL().get(j);
						Line2D.Double lin = new Line2D.Double( pAnt.x, pAnt.y, pAct.x, pAct.y );
						double dist = lin.ptSegDist( p );
						if (dist < CERCANIA_INCLUSION_PUNTOS) {
							return new Object[] { con, j-1 }; 
						}
						pAnt = pAct;
					}
				}
			}
			return null;
		}


	private boolean mostrarCircuitos = true;
	public void mostrarCircuitoYTextos() {
		mostrarCircuitos = true;
		for (TextoDeCircuito tc : tPuntos) {
			tc.setVisible( true );
		}
		pDibujo.repaint();
	}
		
	public void ocultarCircuitoYTextos() {
		mostrarCircuitos = false;
		for (TextoDeCircuito tc : tPuntos) {
			tc.setVisible( false );
		}
		pDibujo.repaint();
	}

	/** Chequea y obliga a que un punto tenga un nombre único en el sistema y que su nombre sea correcto (no empiece por dígito)
	 * @param p
	 * @return	true si es correcto, false si se ha tenido que cambiar
	 */
	public boolean chequearNombrePuntoCorrecto( Punto p ) {
		String nombre = p.getNombre();
		String nombreOriginal = nombre;
		if (nombreOriginal!=null && nombreOriginal.length()>0 && nombreOriginal.charAt(0)>='0' && nombreOriginal.charAt(0)<='9') {  // Si empieza por dígito se hace que empiece por 'P'
			nombre = "P" + nombre; //$NON-NLS-1$
		}
		boolean nombreRepetido;
		do {
			nombreRepetido = false;
			for (Punto p2 : lPuntos) {
				if (p2 != p) {
					if (p2.getNombre().equals( nombre )) {
						// System.out.println( p.hashCode() + " : " + p + " -> " + p.getNombre() + "   " + (p==p2) + " -- " + p.equals(p2) );
						// System.out.println( p2.hashCode() + " : " + p2 + " -> " + p2.getNombre() );
						nombreRepetido = true;
						break;
					}
				}
			}
			if (nombreRepetido) {
				String resp = JOptionPane.showInputDialog( VentanaEditorAveriaAuto.getLastVentana(), Messages.getString("MarcasAverias.163") + nombre + ":",  //$NON-NLS-1$ //$NON-NLS-2$
				 		Messages.getString("MarcasAverias.165"), JOptionPane.QUESTION_MESSAGE ); //$NON-NLS-1$
				if (resp!=null && !resp.equals("")) nombre = resp; //$NON-NLS-1$
			}
		} while (nombreRepetido);
		if (nombreOriginal!=null && !nombreOriginal.equals(nombre)) {
			p.setNombre( nombre );
			return false;
		}
		return true;
	}
	
	/** Chequea un punto dado y devuelve todas conexiones, conectores, o componentes a los que pertenezca.
	 * @param pt	Punto a chequear
	 * @param incluyeResistencias	true si se quieren incluir las resistencias a las que pertenece ese punto, false en caso contrario
	 * @return	Lista de componentes, conectores, conexiiones o resistencias en las que ese punto está
	 */
	public ArrayList<ObjetoDeGrafico> puntoEnQueComponentes( Punto pt, boolean incluyeResistencias ) {
		ArrayList<ObjetoDeGrafico> ret = new ArrayList<>();
		for (Conexion c : lConexiones) if (c.getAL().contains( pt )) ret.add( c );
		for (Conector c : lConectores) if (c.getAL().contains( pt )) ret.add( c );
		for (Componente c : lComponentes) if (c.getAL().contains( pt )) ret.add( c );
		if (incluyeResistencias) for (Resistencia r : lResistencias) if (r.getAL().contains( pt )) ret.add( r );
		return ret;
	}
	
}
