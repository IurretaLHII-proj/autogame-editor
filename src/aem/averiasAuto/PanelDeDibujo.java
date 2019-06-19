package aem.averiasAuto;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import aem.averiasAuto.elem.Componente;
import aem.averiasAuto.elem.Conector;
import aem.averiasAuto.elem.Conexion;
import aem.averiasAuto.elem.Punto;

@SuppressWarnings("serial")
public class PanelDeDibujo extends JPanel {
	private static int transparenciaFondo = 180; // 0 = imagen tal cual, 255 = completamente transparente
	private static double zoom = 1.0;  // 100% de zoom al principio
	private static int zoomEntero = 512;
	private ImageIcon imagen;
	private MarcasAverias marcasAverias;
	private String lastFicheroGrafico;
	
	/** Construye el panel de dibujo
	 * @param dibujo	Objeto de dibujo de circuito (marcas averías) asociado
	 */
	public PanelDeDibujo( MarcasAverias dibujo ) {
		marcasAverias = dibujo;
		if (dibujo.nomFicheroCircuito==null || dibujo.nomFicheroCircuito.equals("")) //$NON-NLS-1$
			dibujo.nomFicheroCircuito = pedirFicheroGrafico();
		try {
			imagen = new ImageIcon( dibujo.nomFicheroCircuito );
			if (imagen.getIconHeight()==-1) throw new NullPointerException();
			lastFicheroGrafico = dibujo.nomFicheroCircuito;
			int anchura = (int) Math.round( imagen.getIconWidth() * zoom );
			int altura = (int) Math.round( imagen.getIconHeight() * zoom );
			setPreferredSize( new Dimension( anchura, altura ));
		} catch (Exception e) {
			System.err.println( Messages.getString("PanelDeDibujo.1") + dibujo.nomFicheroCircuito + Messages.getString("PanelDeDibujo.0") ); //$NON-NLS-1$ //$NON-NLS-2$
			imagen = null;
			lastFicheroGrafico = null;
			setName( (dibujo.nomFicheroCircuito==null) ? "null" : (new File(dibujo.nomFicheroCircuito)).getName() ); //$NON-NLS-1$
		}
		setLayout( null );
	}
	
	/** Devuelve el nombre del último fichero gráfico cargado
	 * @return
	 */
	public String getUltimoFicheroGraficoCargado() {
		return lastFicheroGrafico;
	}
	
	// TODO Gestionar el error de carga de imagen no existente bien en este método o en los llamantes para que pida interactivamente un fichero correcto
	/** Cambia el fondo gráfico por uno nuevo
	 * @param nomFicheroCircuito	Nuevo fichero gráfico
	 */
	public void setFondoImagen( String nomFicheroCircuito ) {
		try {
			nomFicheroCircuito = nomFicheroCircuito.replaceAll( "\\\\", "/" ); //$NON-NLS-1$ //$NON-NLS-2$
			ImageIcon nuevaImagen = new ImageIcon( nomFicheroCircuito );
			lastFicheroGrafico = null;
			if (!(new File(nomFicheroCircuito)).exists()) throw new NullPointerException();
			lastFicheroGrafico = nomFicheroCircuito;
			// if (imagen==null) throw new NullPointerException();
			// if (imagen.getIconHeight()==-1) throw new NullPointerException();  // por alguna razón esto a veces devuelve -1  (será por el preload)
			// Cálculo de márgenes con la nueva imagen
			int maxAncho = nuevaImagen.getIconWidth();
			int maxAlto = nuevaImagen.getIconHeight();
			for (Punto p : marcasAverias.getPuntos()) { if (p.getX()>maxAncho) maxAncho = (int) p.getX(); if (p.getY()>maxAlto) maxAlto = (int) p.getY(); } 
			for (Conexion cx : marcasAverias.getConexiones()) { 
				for (Punto p : cx.getAL()) { if (p.getX()>maxAncho) maxAncho = (int) p.getX(); if (p.getY()>maxAlto) maxAlto = (int) p.getY(); }
			}
			for (Conector cx : marcasAverias.getConectores()) { 
				for (Punto p : cx.getAL()) { if (p.getX()>maxAncho) maxAncho = (int) p.getX(); if (p.getY()>maxAlto) maxAlto = (int) p.getY(); }
			}
			for (Componente cp : marcasAverias.getComponentes()) { 
				for (Punto p : cp.getAL()) { if (p.getX()>maxAncho) maxAncho = (int) p.getX(); if (p.getY()>maxAlto) maxAlto = (int) p.getY(); }
			}
			for (TextoDeCircuito tc : marcasAverias.getTextos()) { 
				if (tc.getX()+tc.getWidth()>maxAncho) maxAncho = tc.getX()+tc.getWidth(); 
				if (tc.getY()+tc.getHeight()>maxAlto) maxAlto = tc.getY()+tc.getHeight();
			}
			maxAncho = (int) (maxAncho * zoom);
			maxAlto = (int) (maxAlto * zoom);
			setPreferredSize( new Dimension( maxAncho, maxAlto ));
			marcasAverias.nomFicheroCircuito = nomFicheroCircuito;
			VentanaEditorAveriaAuto.getLastVentana().tfConfigAncho.setText( ""+nuevaImagen.getIconWidth() ); //$NON-NLS-1$
			VentanaEditorAveriaAuto.getLastVentana().tfConfigAlto.setText( ""+nuevaImagen.getIconHeight() ); //$NON-NLS-1$
			imagen = nuevaImagen;
			cambiaNombreFicheroAlCargarImagen( nomFicheroCircuito );
			revalidate();
		} catch (Exception e) {
			if (!nomFicheroCircuito.equals("")) { //$NON-NLS-1$
				System.err.println( Messages.getString("PanelDeDibujo.9") + nomFicheroCircuito + Messages.getString("PanelDeDibujo.10") ); //$NON-NLS-1$ //$NON-NLS-2$
				e.printStackTrace();
			}
		}
	}
		private void cambiaNombreFicheroAlCargarImagen( String nom ) {
			String ultNomFich = ControladorVentanaEAA.getProperty( "ULTIMO-FIC" ); //$NON-NLS-1$
			if (ultNomFich==null || ultNomFich.equals("")) { //$NON-NLS-1$
			// if (ultNomFich!=null && !ultNomFich.equals("") && ultNomFich.toUpperCase().endsWith( ".AUTOG" )) {
				String nomGrafico = ""; //$NON-NLS-1$
				int posPtoExtension = nom.lastIndexOf( "." ); //$NON-NLS-1$
				if (posPtoExtension>-1) {
					int posBarra = nom.lastIndexOf( "/" ); //$NON-NLS-1$
					if (posBarra==-1) posBarra = nom.lastIndexOf("\\"); //$NON-NLS-1$
					if (posBarra>-1) {
						nomGrafico = nom.substring( posBarra+1, posPtoExtension );
						int posBarraEnFic = ultNomFich.lastIndexOf( "/" ); //$NON-NLS-1$
						if (posBarraEnFic==-1) posBarraEnFic = ultNomFich.lastIndexOf( "\\" ); //$NON-NLS-1$
						if (posBarraEnFic>-1) {
							String nomFich = ultNomFich.substring(0, posBarraEnFic+1 );
							nomFich = nomFich + nomGrafico + ".autoG";  //$NON-NLS-1$
							ControladorVentanaEAA.getProperties().setProperty( "ULTIMO-FIC", nomFich ); //$NON-NLS-1$
						}
					}
				}
			}
		}
	
	public void setTransparencia( int valTransp ) {
		if (valTransp>=0 && valTransp <=255) transparenciaFondo = valTransp;
	}
	
	/** Pone el zoom
	 * @param valZoom  de 0 (25%) a 1024 (400%)
	 */
	public void setZoom( int valZoom ) {
		zoomEntero = valZoom;
		if (valZoom == 512) {
			zoom = 1.0;
		} else if (valZoom < 512) {
			zoom = 0.5 + valZoom/512.0*0.5;    // Cambiar el primero y el último valor. Deben sumar 1. Minimo zoom = primer valor
		} else {
			zoom = 1.0 + (valZoom-512)/512.0*1.0;  // Cambiar este último valor. Máximo zoom = 1+valor
		}
		int maxAncho = imagen.getIconWidth();
		int maxAlto = imagen.getIconHeight();
		for (Punto p : marcasAverias.getPuntos()) { if (p.getX()>maxAncho) maxAncho = (int) p.getX(); if (p.getY()>maxAlto) maxAlto = (int) p.getY(); } 
		for (Conexion cx : marcasAverias.getConexiones()) { 
			for (Punto p : cx.getAL()) { if (p.getX()>maxAncho) maxAncho = (int) p.getX(); if (p.getY()>maxAlto) maxAlto = (int) p.getY(); }
		}
		for (Conector cx : marcasAverias.getConectores()) { 
			for (Punto p : cx.getAL()) { if (p.getX()>maxAncho) maxAncho = (int) p.getX(); if (p.getY()>maxAlto) maxAlto = (int) p.getY(); }
		}
		for (Componente cp : marcasAverias.getComponentes()) { 
			for (Punto p : cp.getAL()) { if (p.getX()>maxAncho) maxAncho = (int) p.getX(); if (p.getY()>maxAlto) maxAlto = (int) p.getY(); }
		}
		// Recolocar los textos de circuito
		relocateTextosDeCircuito( zoom );
		for (TextoDeCircuito tc : marcasAverias.getTextos()) { 
			if (tc.getX()+tc.getWidth()>maxAncho) maxAncho = tc.getX()+tc.getWidth(); 
			if (tc.getY()+tc.getHeight()>maxAlto) maxAlto = tc.getY()+tc.getHeight();
		}
		int anchura = (int) Math.round( maxAncho * zoom );
		int altura = (int) Math.round( maxAlto * zoom );
		setPreferredSize( new Dimension( anchura, altura ));
		getParent().revalidate();
	}
		private void relocateTextosDeCircuito( double zoom ) {
			for (TextoDeCircuito tc : marcasAverias.getTextos()) {
				tc.setLocationConZoom( zoom );
			}
		}
		
	
	public int getZoomEntero() { return zoomEntero; }
	public double getZoom() { return zoom; }
	
	/** Devuelve una coordenada de pantalla (real) partiendo de una coordenada lógica, dado el zoom actual
	 * @param coord
	 * @return
	 */
	public int getCoordPant( int coord ) {
		return (int)Math.round(coord * zoom);
	}
	
	/** Devuelve una coordenada lógica partiendo de una coordenada de pantalla (real), dado el zoom actual
	 * @param coord
	 * @return
	 */
	public int getCoordLogica( int coord ) {
		return (int)Math.round(coord / zoom);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;  // El Graphics realmente es Graphics2D
		if (imagen!=null) {
			Image img = imagen.getImage();
			// Escalado más fino con estos 3 parámetros:
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);	
	        // Dibujado de la imagen
			int anchura = (int) Math.round( imagen.getIconWidth() * zoom );
			int altura = (int) Math.round( imagen.getIconHeight() * zoom );
	        g2.drawImage( img, 0, 0, anchura, altura, null );
	        g2.setColor( new Color( 255, 255, 255, transparenciaFondo ));
	        g2.fillRect( 0, 0, anchura, altura );
		} else {
			super.paintComponent(g);
			g2.setColor( Color.blue );
			g2.drawString( getName(), 10, 10 );
		}
		if (marcasAverias!=null) marcasAverias.dibuja( g2 );
	}

		// Pide interactivamente un fichero gráfico
		// (null si no se selecciona)
		private String pedirFicheroGrafico() {
			File dirActual = new File( ControladorVentanaEAA.getUltimoDirectorio() );
			JFileChooser chooser = new JFileChooser( dirActual );
			chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
			chooser.setFileFilter( new FileNameExtensionFilter( 
					Messages.getString("PanelDeDibujo.21"), "jpg", "png", "gif" ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			chooser.setDialogTitle( Messages.getString("PanelDeDibujo.25") ); //$NON-NLS-1$
			int returnVal = chooser.showOpenDialog( null );
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				ControladorVentanaEAA.actualizarUltimoDir( chooser.getSelectedFile() );
				return chooser.getSelectedFile().getAbsolutePath();
			} else 
				return null;
		}

	// 
	// Rutinas públicas de dibujado
	// 
		
	public static void dibujaCuadraditoEnMedio( Graphics2D g, Point p1, Point p2, double zoom ) {
		MarcasAverias.dibujaRect( g, new Point( (p1.x+p2.x)/2, (p1.y+p2.y)/2 ), 
				MarcasAverias.ANCHO_MEDIO_MARCA_COMP*2, MarcasAverias.ANCHO_MEDIO_MARCA_COMP*2, zoom );
	}
	public static void dibujaCirculitoEnMedio( Graphics2D g, Point p1, Point p2, double zoom ) {
		dibujaCirculitoEnMedio( g, p1, p2, zoom, MarcasAverias.ANCHO_MEDIO_MARCA_COMP );
	}
	public static void dibujaCirculitoEnMedio( Graphics2D g, Point p1, Point p2, double zoom, int radio ) {
		dibujaCirculitoEnMedio( g, p1, p2, zoom, radio, false );
	}
	public static void dibujaCirculitoEnMedio( Graphics2D g, Point p1, Point p2, double zoom, int radio, boolean rellenoBlanco ) {
		int pto1X = (int)Math.round((p1.x+p2.x)/2*zoom)-radio;
		int pto1Y = (int)Math.round((p1.y+p2.y)/2*zoom)-radio;
		if (rellenoBlanco) {
			Color c = g.getColor();
			g.setColor( Color.white );
			g.fillOval( pto1X, pto1Y, (int)Math.round(radio*2), (int)Math.round(radio*2));
			g.setColor( c );
		}
		g.drawOval( pto1X, pto1Y, (int)Math.round(radio*2), (int)Math.round(radio*2));
	}
	public static void dibujaSimboloEnMedio( Graphics2D g, Point p1, Point p2, double zoom, String tipoComp ) {
		int centro1X = (int)Math.round((p1.x+p2.x)/2*zoom)-MarcasAverias.ANCHO_MEDIO_MARCA_COMP;
		int centro1Y = (int)Math.round((p1.y+p2.y)/2*zoom)-MarcasAverias.ANCHO_MEDIO_MARCA_COMP;
		double distancia = p1.distance(p2) * zoom;
		double angulo = - Math.atan2( p2.x - p1.x, p2.y - p1.y ) + Math.PI / 2;
		if (p1==p2) angulo = 0;
		int tamanyo = MarcasAverias.ANCHO_MEDIO_MARCA_COMP*4;
		if (p1!=p2 && tamanyo > distancia*0.75) tamanyo = (int) Math.round( distancia*0.75 );  // Se pone al tamaño estándar salvo que no haya espacio y entonces se reduce
		if ("#Fusible#Bateria#Masa#Bocina#Rele#Int2#Motor#Lampara#Resistencia#ResistenciaInfinita#Potenciometro2#".contains(tipoComp)) { //$NON-NLS-1$
			try {
				BufferedImage dibujo = ImageIO.read( EditorAveriasAuto.class.getResource( "img/comp/" + tipoComp + ".png" ).toURI().toURL() ); //$NON-NLS-1$ //$NON-NLS-2$
				double escala = 1.0 * tamanyo / dibujo.getWidth();
				AffineTransform at = new AffineTransform();  // create the transform, note that the transformations happen in reversed order (so check them backwards)
				at.translate( centro1X + tamanyo/4, centro1Y + tamanyo/4 );  // 4. translate it to the center of the component
				at.rotate( angulo ); // 3. do the actual rotation
				at.scale( escala, escala ); // 2. just a scale because this image is big
				at.translate( -dibujo.getWidth()/2, -dibujo.getHeight()/2); // 1. translate the object so that you rotate it around the center (easier :))
				g.drawImage( dibujo, at, null ); // draw the image
			} catch (Exception e) {} 
		}
	}
	public static void dibujaSimboloEntrePuntos( Graphics2D g, Point p1, Point p2, double zoom, String tipoComp ) {
		int centro1X = (int)Math.round((p1.x+p2.x)/2*zoom)-MarcasAverias.ANCHO_MEDIO_MARCA_COMP;
		int centro1Y = (int)Math.round((p1.y+p2.y)/2*zoom)-MarcasAverias.ANCHO_MEDIO_MARCA_COMP;
		double distancia = p1.distance(p2) * zoom;
		double angulo = - Math.atan2( p2.x - p1.x, p2.y - p1.y ) + Math.PI / 2;
		if (p1==p2) angulo = 0;
		int tamanyoVert = MarcasAverias.ANCHO_MEDIO_MARCA_COMP*4;
		if ("#Fusible#Bateria#Masa#Bocina#Rele#Int2#Motor#Lampara#Resistencia#ResistenciaInfinita".contains(tipoComp)) { //$NON-NLS-1$
			try {
				BufferedImage dibujo = ImageIO.read( EditorAveriasAuto.class.getResource( "img/comp/" + tipoComp + ".png" ).toURI().toURL() ); //$NON-NLS-1$ //$NON-NLS-2$
				double escalaHor = 1.0 * distancia / dibujo.getHeight();
				double escalaVer = 1.0 * tamanyoVert / dibujo.getWidth();
				AffineTransform at = new AffineTransform();  // create the transform, note that the transformations happen in reversed order (so check them backwards)
				at.translate( centro1X + distancia/4, centro1Y + tamanyoVert/4 );  // 4. translate it to the center of the component
				at.rotate( angulo ); // 3. do the actual rotation
				at.scale( escalaHor, escalaVer ); // 2. just a scale because this image is big
				at.translate( -dibujo.getWidth()/2, -dibujo.getHeight()/2); // 1. translate the object so that you rotate it around the center (easier :))
				g.drawImage( dibujo, at, null ); // draw the image
			} catch (Exception e) {} 
		}
	}
	public static Point getPuntoMedio( Point p1, Point p2 ) {
		return new Point( (p1.x+p2.x)/2, (p1.y+p2.y)/2 );
	}
	public static Point getPuntoEnMedio( Point... ptos ) {
		int xMin, xMax, yMin, yMax;
		if (ptos.length==0) return null;
		xMin = 10000; xMax = -10000; yMin = 10000; yMax = -10000;
		for (Point p : ptos) {
			if (p.x < xMin) xMin = p.x;
			if (p.x > xMax) xMax = p.x;
			if (p.y < yMin) yMin = p.y;
			if (p.y > yMax) yMax = p.y;
		}
		return new Point( (xMin+xMax)/2, (yMin+yMax)/2 );
	}
	public static int getAnchoAlto( boolean maximo, Point... ptos ) {
		int xMin, xMax, yMin, yMax;
		if (ptos.length==0) return 0;
		xMin = 10000; xMax = -10000; yMin = 10000; yMax = -10000;
		for (Point p : ptos) {
			if (p.x < xMin) xMin = p.x;
			if (p.x > xMax) xMax = p.x;
			if (p.y < yMin) yMin = p.y;
			if (p.y > yMax) yMax = p.y;
		}
		int ancho = xMax-xMin; int alto = yMax-yMin;
		if (maximo)
			return (ancho<alto) ? alto : ancho;
		else
			return (ancho>alto) ? alto : ancho;
	}

		
		
}
