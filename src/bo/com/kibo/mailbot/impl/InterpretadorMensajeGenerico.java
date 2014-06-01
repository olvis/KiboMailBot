/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.exceptions.BusinessException;
import bo.com.kibo.bl.exceptions.BusinessExceptionMessage;
import bo.com.kibo.bl.intf.IGenericoBO;
import bo.com.kibo.mailbot.intf.IInterpretadorMensaje;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 *
 * @author Olvinho
 * @param <T> Clase entidad
 * @param <BO> Clase de negocio
 */
public abstract class InterpretadorMensajeGenerico<T, BO extends IGenericoBO<T, ?>> implements IInterpretadorMensaje {

    private static final ThreadLocal<Map<String, IInterpretadorMensaje>> caja = new ThreadLocal<>();

    public static Map<String, IInterpretadorMensaje> getMapaObjetos() {
        Map<String, IInterpretadorMensaje> mapa = caja.get();
        if (mapa == null) {
            mapa = new HashMap<>();
            mapa.put("area", new InterpretadorMensajeArea());

            caja.set(mapa);
        }
        return mapa;
    }

    protected Integer idUsuario;
    protected String parametros;
    protected String nombreEntidad;
    protected List<File> archivosTemporales = new ArrayList<>();
    protected Sheet hojaActual;
    private BusinessException mensajesError;

    public InterpretadorMensajeGenerico() {
        parametros = "";
    }

    @Override
    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    @Override
    public void setParametros(String parametros) {
        this.parametros = parametros;
    }

    @Override
    public Multipart interpretar() throws MessagingException, IOException {
        if (parametros == null || "".equals(parametros)) {
            return null;
        }
        String[] params = parametros.split(UtilitariosMensajes.SEPERADOR_PARAMETROS + "");
        String idCargar = "";
        if (params.length > 1)
            idCargar = params[1];
        switch (params[0]) {
            case "plantilla":
                return enviarPlantilla(true, idCargar);
            case "cargar":
               return enviarPlantilla(false, idCargar);
        }
        return null;
    }

    protected void appendException(BusinessExceptionMessage message) {
        if (mensajesError == null) {
            mensajesError = new BusinessException(message);
            return;
        }
        mensajesError.getMessages().add(message);
    }

    private Multipart enviarPlantilla(boolean plantillaNueva, String idCargar) throws MessagingException, IOException {
        Multipart cuerpo = new MimeMultipart();
        BodyPart mensaje = new MimeBodyPart();
        mensaje.setText("La plantilla de " + nombreEntidad + " para poder insertar está adjunta");
        BodyPart adjunto = new MimeBodyPart();
        String nombreArchivoOriginal = "plantillas\\" + nombreEntidad + ".xlsx";
        File archivoCopia = UtilitariosMensajes.reservarNombre(nombreEntidad);
        UtilitariosMensajes.copiarArchivo(new File(nombreArchivoOriginal), archivoCopia);
        archivosTemporales.add(archivoCopia);
        if (abrirPlantillaAntesDeEnviar()) {
            //Abrir el archivo para hacer las preparaciones necesarias
        }
        DataSource origen = new FileDataSource(archivoCopia);
        adjunto.setDataHandler(new DataHandler(origen));
        adjunto.setFileName(nombreEntidad + ".xlsx");
        cuerpo.addBodyPart(mensaje);
        cuerpo.addBodyPart(adjunto);
        return cuerpo;
    }

    @Override
    public void setNombreEntidad(String nombre) {
        this.nombreEntidad = nombre;
    }

    protected boolean abrirPlantillaAntesDeEnviar() {
        return false;
    }

    @Override
    public List<File> obtenerArchivoTemporalesCreados() {
        return archivosTemporales;
    }

    protected Cell getCelda(int rowIndex, int colIndex) {
        Row fila = hojaActual.getRow(rowIndex);
        if (fila == null) {
            fila = hojaActual.createRow(rowIndex);
        }

        Cell celda = fila.getCell(colIndex);
        if (celda == null) {
            celda = fila.createCell(colIndex);
        }

        return celda;

    }

    protected Multipart enviarErroresNegocio(BusinessException errores) throws MessagingException {
        Multipart cuerpo = new MimeMultipart();
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("No se pudo completar la petición debido a los siguiente errores: \n");
        for (BusinessExceptionMessage error : errores.getMessages()) {
            mensaje.append("-");
            if (error.getIndex() > 0) {
                mensaje.append("Fila ").append(error.getIndex()).append(" --> ");
            }
            mensaje.append(error.getMessage()).append("\n");
        }
        BodyPart texto = new MimeBodyPart();
        texto.setText(mensaje.toString());
        cuerpo.addBodyPart(texto);
        return cuerpo;
    }

    protected Multipart enviarErrorInesperado() throws MessagingException {
        Multipart cuerpo = new MimeMultipart();
        BodyPart texto = new MimeBodyPart();
        texto.setText("Ha ocurrido un error inesperado, por favor intentelo nuevamente");
        cuerpo.addBodyPart(texto);
        return cuerpo;
    }

    protected Multipart enviarInserccionExitosa(T entidad) throws MessagingException {
        Multipart cuerpo = new MimeMultipart();
        BodyPart texto = new MimeBodyPart();
        texto.setText("La insercción se ha efectuado exitosamente. El identificador del registro es: " + getId(entidad));
        cuerpo.addBodyPart(texto);
        return cuerpo;

    }

    protected Multipart enviarModificacionExitosa(T entidad) throws MessagingException {
        Multipart cuerpo = new MimeMultipart();
        BodyPart texto = new MimeBodyPart();
        texto.setText("Se ha efectuado la actualización exitosamente");
        cuerpo.addBodyPart(texto);
        return cuerpo;
    }

    @Override
    public Multipart interpretarHojaExcel(Sheet hojaExcel) throws MessagingException {
        this.hojaActual = hojaExcel;
        mensajesError = null;
        T entidad = convertirHojaEnEntidad();
        if (mensajesError != null) {
            return enviarErroresNegocio(mensajesError);
        }
        getObjetoNegocio().setIdUsuario(idUsuario);
        boolean esInserccion;
        try {
            if (esNuevo(entidad)) {
                esInserccion = true;
                getObjetoNegocio().insertar(entidad);
            } else {
                esInserccion = false;
                getObjetoNegocio().actualizar(entidad);

            }
        } catch (BusinessException e) {
            return enviarErroresNegocio(e);
        } catch (Exception e) {
            return enviarErrorInesperado();
        }

        if (esInserccion) {
            return enviarInserccionExitosa(entidad);
        }
        return enviarModificacionExitosa(entidad);
    }

    abstract T convertirHojaEnEntidad();

    abstract BO getObjetoNegocio();

    abstract boolean esNuevo(T entidad);

    abstract String getId(T entidad);

}
