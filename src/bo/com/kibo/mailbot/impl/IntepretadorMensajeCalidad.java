/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.ICalidadBO;
import bo.com.kibo.entidades.Calidad;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;

/**
 *
 * @author Olvinho
 */
public class IntepretadorMensajeCalidad extends InterpretadorMensajeGenerico<Calidad, Integer, ICalidadBO> {

    @Override
    Calidad convertirHojaEnEntidad() {
        Calidad entidad = new Calidad();
        Cell celda;
        //Id
        celda = getCelda(3, 2);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setId((int) celda.getNumericCellValue());
        }

        //Codigo
        celda = getCelda(4, 2);
        entidad.setCodigo(getValorCeldaCadena(celda));

        //Descripcion
        celda = getCelda(5, 2);
        entidad.setDescripcion(getValorCeldaCadena(celda));

        return entidad;
    }

    @Override
    ICalidadBO getObjetoNegocio() {
        return FactoriaObjetosNegocio.getInstance().getCalidadBO();
    }

    @Override
    boolean esNuevo(Calidad entidad) {
        return (entidad.getId() == null);
    }

    @Override
    String getId(Calidad entidad) {
        return entidad.getId().toString();
    }

    @Override
    Integer convertirId(String cadena) throws Exception {
        return convertirIdAEntero(cadena);
    }

    @Override
    void mostrarLista(List<Calidad> lista) {
        int i = 5;
        for (Calidad c : lista) {
            setValorCelda(i, 1, c.getId());
            setValorCelda(i, 2, c.getCodigo());
            setValorCelda(i, 3, c.getDescripcion());
            i++;
        }
    }

    @Override
    void mostrarEntidad(Calidad entidad) {
        setValorCelda(3, 2, entidad.getId());
        setValorCelda(4, 2, entidad.getCodigo());
        setValorCelda(5, 2, entidad.getDescripcion());
    }

}
