package aem.averiasAuto.elem;

public enum TipoElemento {
	Punto, Conexion, Conector, Componente, Resistencia;
	private static String[] strings = { "Punto", "Conexion", "Conector", "Componente", "Resistencia" };
	public static String[] getStrings() { return strings; }
}
