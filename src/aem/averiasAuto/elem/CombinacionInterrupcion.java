package aem.averiasAuto.elem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.StringTokenizer;

import aem.averiasAuto.MarcasAverias;

public class CombinacionInterrupcion implements Serializable {
	private static final long serialVersionUID = 1L;
	private ArrayList<Componente> lInterruptores = new ArrayList<Componente>();
	private ArrayList<ArrayList<String>> lEstados = new ArrayList<ArrayList<String>>();
	
	/** Constructor especial - combinación de interrupción "Todas" para selección múltiple
	 */
	public CombinacionInterrupcion() {
		lInterruptores = null;
		lEstados = null;
	}
	
	public boolean isCombinacionTodas() {
		return lInterruptores==null;
	}
	
	/** Construye una nueva combinación de interrupción
	 * Si hay algún error genera una excepción NullPointerException
	 * @param cI	Formato: nombreInt,nomEstado | nombreInt,nomEstado ...
	 */
	public CombinacionInterrupcion( String cI, MarcasAverias ma ) throws NullPointerException {
		StringTokenizer st = new StringTokenizer( cI, "|" );
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			tok = tok.trim();
			int hayComa = tok.indexOf(",");
			ArrayList<String> lEsts = new ArrayList<String>();
			String nomInt;
			if (hayComa==-1) {   // Formato antiguo   nombreIntN
				String est = tok.substring( tok.length()-1, tok.length() );
				nomInt = tok.substring( 0, tok.length()-1 );
				lEsts.add( est );
			} else {   // Formato nuevo v1.08 nombreInt,estado
				StringTokenizer st2 = new StringTokenizer( tok, "," );
				nomInt = st2.nextToken();  // El primero es el nombre del interruptor
				while (st2.hasMoreTokens()) {  // El resto los estados
					lEsts.add( st2.nextToken() );
				}
			}
			for (Componente comp : ma.getComponentes()) {
				if (comp.tipo.startsWith("Int") && comp.nombre.equalsIgnoreCase( nomInt )) {
					lInterruptores.add( comp );
					lEstados.add( lEsts );
					break;
				}
			}
		}
	}
	
	/** Crea una nueva combinación de interrupción con un solo interruptor. Se puede ampliar con el método addCombinacion
	 * @param interruptor	Interruptor de la combinación
	 * @param estados	Estados del interruptor con la misma distribución eléctrica
	 */
	public CombinacionInterrupcion( Componente interruptor, ArrayList<String> estados ) {
		addCombinacion(interruptor, estados );
	}
	
	/** Añade una nueva combinación de interrupción al objeto actual.
	 * @param interruptor	Nuevo interruptor de la combinación (debe ser nuevo)
	 * @param estados	Estados del interruptor con la misma distribución eléctrica
	 */
	public void addCombinacion( Componente interruptor, ArrayList<String> estados ) {
		lInterruptores.add( interruptor );
		lEstados.add( estados );
	}
	
	public ArrayList<Componente> getLInterruptores() { return lInterruptores; }
	public ArrayList<ArrayList<String>> getLEstados() { return lEstados; }
	/** Devuelve la lista de estados de un interruptor
	 * @param interr	Interruptor del circuito
	 * @return	Lista de estados de ese interruptor, null si no existe en la combinación
	 */
	public ArrayList<String> getLEstadosDeInt( Componente interr ) {
		int posInt = lInterruptores.indexOf( interr );
		if (posInt == -1) return null;
		return lEstados.get(posInt);
	}
	
	@Override
	public String toString() {
		String ret = "";
		if (lInterruptores==null) return "Todas";
		for (int i=0; i<lInterruptores.size(); i++) {
			ret = ret + lInterruptores.get(i).nombre;
			for (String s : lEstados.get(i)) ret = ret + "," + s;
			if (i<lInterruptores.size()-1) ret = ret + " | ";
		}
		return ret;
	}
	
}
