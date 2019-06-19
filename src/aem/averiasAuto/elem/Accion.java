package aem.averiasAuto.elem;

/**  Acción que puede hacerse o deshacerse
 * @author Andoni Eguíluz Morán
 * Facultad de Ingeniería - Universidad de Deusto
 */
public interface Accion {

	/* Si hiciera falta que cada elemento supiera hacerse y deshacerse a sí mismo...
	 * Pero delegamos la responsabilidad de hacer y deshacer a MarcasAverias que tiene la visión global

		/** Hacer una acción -crear, borrar, cambiar-
		 * @param marcas	Marcas de averías sobre las que se actúa
		 * @param tipo	Crear, borrar o cambiar
		 * @param accion2	(null si es crear)
		void hacer( MarcasAverias marcas, Acciones.TipoAccion tipo, Accion accion2 );
		
		/** Deshacer una acción -crear, borrar, cambiar-
		 * @param marcas	Marcas de averías sobre las que se actúa
		 * @param tipo	Crear, borrar o cambiar
		 * @param accion2	(null si es borrar)
		void deshacer( MarcasAverias marcas, Acciones.TipoAccion tipo, Accion accion2 );
	
	 */
}
