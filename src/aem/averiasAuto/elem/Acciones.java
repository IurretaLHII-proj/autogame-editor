package aem.averiasAuto.elem;

import java.util.LinkedList;

import aem.averiasAuto.ControladorVentanaEAA;
import aem.averiasAuto.MarcasAverias;

/** Clase para acciones que se registran y se pueden deshacer.
 * Basada en patrón singleton
 * @author Andoni Eguíluz Morán
 * Facultad de Ingeniería - Universidad de Deusto
 */
public class Acciones {
	
	private static boolean DEBUG = false;
	
	/** Tipos de acciones que se realizan con cada uno de los elementos
	 */
	public static enum TipoAccion { CREAR, BORRAR, CAMBIAR }; 
	
	private static Acciones singleton = new Acciones();
	/** Devuelve el único objeto de acciones que existe (patrón singleton)
	 * @return	Objeto de acciones
	 */
	public static Acciones getInstance() {
		return singleton;
	}
	
	// Estructuras para acciones realizadas que utiliza DESHACER
	private LinkedList<LinkedList<Accion>> elementosAccion;
	private LinkedList<LinkedList<TipoAccion>> tiposAccion;
	private LinkedList<String> nombresAccion;
	// Estructuras para acciones realizadas que utiliza REHACER
	private LinkedList<LinkedList<Accion>> rehElementosAccion;
	private LinkedList<LinkedList<TipoAccion>> rehTiposAccion;
	private LinkedList<String> rehNombresAccion;
	
	private MarcasAverias marcas;
	
	// Constructor al que no se llama nunca desde fuera
	private Acciones() {
	}

	/** Reinicia el sistema de acciones (debe llamársele al menos una vez al inicio)
	 */
	public void reset( MarcasAverias marcas ) {
		elementosAccion = new LinkedList<>();
		tiposAccion = new LinkedList<>();
		nombresAccion = new LinkedList<>();
		rehElementosAccion = new LinkedList<>();
		rehTiposAccion = new LinkedList<>();
		rehNombresAccion = new LinkedList<>();
		this.marcas = marcas;
	}
	
	/** Crea una nueva acción "do" vacía
	 * @param nombreAccion
	 */
	public void hacer( String nombreAccion ) {
		nombresAccion.push( nombreAccion );
		elementosAccion.push( new LinkedList<Accion>() );
		tiposAccion.push( new LinkedList<TipoAccion>() );
	}
	
	/** Añade al último "do" una acción concreta
	 * @param tipo	crear, borrar o cambiar
	 * @param accion	Acción que se crea (crear), que se borra (borrar) o a la final a la que se cambia (cambiar)
	 * @param accion2	Acción inicial que se cambia (cambiar), o null (crear o borrar)
	 */
	public void hacer( TipoAccion tipo, Accion accion, Accion accion2 ) {
		elementosAccion.getFirst().add( accion );
		tiposAccion.getFirst().add( tipo );
		if (tipo==TipoAccion.CAMBIAR)
			elementosAccion.getFirst().add( accion2 );
		else
			elementosAccion.getFirst().add( null );
		if (DEBUG) {
			System.out.println( "PILA DE HACER: ");
			System.out.println( "  " + nombresAccion );
			System.out.println( "  " + tiposAccion );
			System.out.println( "  " + elementosAccion );
		}
	}
	
	/** Deshace la última acción, si existe
	 */
	public void deshacer() { 
		if (DEBUG) System.out.println( "DESHACER!!!");
		if (!nombresAccion.isEmpty()) {
			String nomAc = nombresAccion.pop();
			LinkedList<Accion> elementos = elementosAccion.pop();
			LinkedList<TipoAccion> tipos = tiposAccion.pop();
			rehNombresAccion.push( nomAc );
			rehElementosAccion.push( elementos );
			rehTiposAccion.push( tipos );
			for (TipoAccion tipo : tipos) {
				Accion accion1 = elementos.pop();
				Accion accion2 = elementos.pop();
				marcas.deshacer( tipo, accion1, accion2 );
			}
		}
		marcas.getPanelDibujo().repaint();
		ControladorVentanaEAA.getLast().cargarTabla();  // Refresca la tabla derecha
		if (DEBUG) {
			System.out.println( "  " + nombresAccion );
			System.out.println( "  " + tiposAccion );
			System.out.println( "  " + elementosAccion );
		}
	}
	
	/** Rehace la última acción deshecha, si existe
	 */
	public void rehacer() {
		if (DEBUG) System.out.println( "REHACER!!!");
		if (!rehNombresAccion.isEmpty()) {
			String nomAc = rehNombresAccion.pop();
			LinkedList<Accion> elementos = rehElementosAccion.pop();
			LinkedList<TipoAccion> tipos = rehTiposAccion.pop();
			nombresAccion.push( nomAc );
			elementosAccion.push( elementos );
			tiposAccion.push( tipos );
			for (TipoAccion tipo : tipos) {
				Accion accion1 = elementos.pop();
				Accion accion2 = elementos.pop();
				marcas.hacer( tipo, accion1, accion2 );
			}
			marcas.getPanelDibujo().repaint();
		}
	}
	
	/** Elimina la última acción, si es vacía (si no tiene elementos de acción en ella)
	 */
	public void limpiarSiNoAccion() {
		if (!nombresAccion.isEmpty()) {
			if (tiposAccion.getFirst().isEmpty()) {
				nombresAccion.pop();
				tiposAccion.pop();
				elementosAccion.pop();
			}
		}
	}
	
}
