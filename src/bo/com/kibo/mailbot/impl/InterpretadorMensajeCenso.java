/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.IFormularioCensoBO;
import bo.com.kibo.entidades.Area;
import bo.com.kibo.entidades.Calidad;
import bo.com.kibo.entidades.Especie;
import bo.com.kibo.entidades.Faja;
import bo.com.kibo.entidades.FormularioCenso;
import bo.com.kibo.mailbot.intf.IInterpretadorFormularioDasometrico;
import java.util.List;

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
        FormularioCenso entidad = new FormularioCenso();
        
        return entidad;
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
    protected void preparPlantillaAntesDeEnviar() {
        if (super.isCargarPlantillaFormularios()) {
            cargarAreasAPlantillaFormularios();
        } else {
            //Cargamos los datos para preparar la plantilla
            String[] valores;
            int i;
            //Areas
            setValorCelda(4, 2, area.getCodigo());
            //Fajas
            List<Faja> fajas = FactoriaObjetosNegocio.getInstance().getFajaBO().obtenerFajasSegunArea(area.getId());
            valores = new String[fajas.size()];
            for(i = 0; i < fajas.size(); i++){
                valores[i]= fajas.get(i).getBloque() + "-" + fajas.get(i).getNumero();
            }
            agregarValidacionLista(4, 4, 5, 5, valores, true, true);
            //Especies
            List<Especie> especies = FactoriaObjetosNegocio.getInstance().getEspecieBO().obtenerTodos();
            valores = new String[especies.size()];
            for(i = 0; i < especies.size(); i++){
                valores[i]= especies.get(i).getNombre();
            }
            agregarValidacionLista(8, 37, 2, 2, valores, true, false);
            //Calidades
            List<Calidad> calidades = FactoriaObjetosNegocio.getInstance().getCalidadBO().obtenerTodos();
            valores = new String[calidades.size()];
            for(i = 0; i < calidades.size(); i++){
                valores[i]= calidades.get(i).getCodigo();
            }
            agregarValidacionLista(8, 37, 5, 5, valores, true, false);
        }
    }

}
