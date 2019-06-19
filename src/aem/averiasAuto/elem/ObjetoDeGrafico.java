package aem.averiasAuto.elem;

import javax.swing.JPanel;

import aem.averiasAuto.TextoDeCircuito;

public interface ObjetoDeGrafico extends Accion {
	
	/** Devuelve el tipo del objeto gráfico
	 * @return	Tipo del objeto
	 */
	public TipoElemento getTipoElemento();
	
	/** Devuelve el nombre del objeto gráfico
	 * @return	Nombre del objeto
	 */
	public String getNombre();
	
	/** Devuelve el valor de la propiedad indicada
	 * @param prop	Nombre de la propiedad
	 * @return	Valor de la propiedad
	 */
	public Object getVal( String prop );
	
	/** Modifica el valor de la propiedad indicada
	 * @param prop	Nombre de la propiedad
	 * @param val	Valor de la propiedad
	 */
	public void setVal( String prop, Object val );
	
	/** Devuelve la lista de propiedades del objeto
	 * @return	Array de propiedades
	 */
	public String[] getProps();
	
	/** Devuelve la lista de tipos de propiedades del objeto
	 * @return	Array de tipos de propiedades (en el mismo orden que las propiedades {@link #getProps()})
	 */
	public Class<?>[] getTipos();
	
	/** Convierte el objeto a String en una línea -con atributos separados por comas-
	 * @return	Versión textual del objeto
	 */
	public String aTexto();
	
	/** Convierte el objeto a String en una línea descriptiva para el usuario
	 * @return	Versión textual del objeto para el usuario
	 */
	public String toDescripcion();
	
	/** Devuelve panel de edición de atributos del objeto gráfico
	 * @return	Panel de edición de atributos del objeto gráfico
	 */
	public JPanel getPanelEdicion();

	/** Cambia el texto de circuito asociado al objeto gráfico
	 * @param tdc	Texto de circuito asociado, null si no lo hay
	 */
	public void setTextoAsociado( TextoDeCircuito tdc );
	
	/** Devuelve el texto de circuito asociado al objeto gráfico
	 * @return	Texto de circuito asociado, null si no lo hay
	 */
	public TextoDeCircuito getTextoAsociado();
	
}
