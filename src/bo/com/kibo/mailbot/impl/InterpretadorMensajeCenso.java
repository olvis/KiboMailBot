/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.IFormularioCensoBO;
import bo.com.kibo.entidades.Area;
import bo.com.kibo.entidades.FormularioCenso;
import bo.com.kibo.mailbot.intf.IInterpretadorFormularioDasometrico;
import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.Multipart;

/**
 *
 * @author Olvinho
 * @version 1.0
 */
public class InterpretadorMensajeCenso extends InterpretadorMensajeGenerico<FormularioCenso, Integer, IFormularioCensoBO> implements IInterpretadorFormularioDasometrico {

    private Area area;

    @Override
    public void setArea(Area area) {
        this.area = area;
    }

    @Override
    public Area getArea() {
        return area;
    }

    @Override
    FormularioCenso convertirHojaEnEntidad() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    IFormularioCensoBO getObjetoNegocio() {
        return FactoriaObjetosNegocio.getInstance().getFormularioCensoBO();
    }

    @Override
    boolean esNuevo(FormularioCenso entidad) {
        return (entidad.getId() == null);
    }

    @Override
    String getId(FormularioCenso entidad) {
        return entidad.getId().toString();
    }

    @Override
    Integer convertirId(String cadena) throws Exception {
        return convertirIdAEntero(cadena);
    }

    @Override
    void mostrarLista(List<FormularioCenso> lista) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void mostrarEntidad(FormularioCenso entidad) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Multipart enviarPlantilla(boolean plantillaNueva, String idCargar) throws MessagingException, IOException {
        return super.enviarPlantilla(plantillaNueva, idCargar); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void preparPlantillaAntesDeEnviar() {
        if (super.isCargarPlantillaFormularios()) {
            cargarAreasAPlantillaFormularios();
        } else {
            //Cargamos los datos para preparar la plantilla
            
        }
    }

}
