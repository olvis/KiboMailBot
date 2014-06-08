/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.ICargaBO;
import bo.com.kibo.entidades.Carga;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;

/**
 *
 * @author Olvinho
 */
public class InterpretadorMensajeCarga extends InterpretadorMensajeGenerico<Carga, Integer, ICargaBO> {

    @Override
    Carga convertirHojaEnEntidad() {
        Carga entidad = new Carga();
        Cell celda;
        celda = getCelda(3, 2);
        //Id
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setId((int) celda.getNumericCellValue());
        }
        //Para Codigo
        celda = getCelda(4, 2);
        entidad.setCodigo(getValorCeldaCadena(celda));

        //Si es Rama
        celda = getCelda(5, 2);
        if ("si".equals(getValorCeldaCadena(celda).toLowerCase())) {
            entidad.setEsRama(true);
        } else {
            entidad.setEsRama(false);
        }

        return entidad;
    }

    @Override
    ICargaBO getObjetoNegocio() {
        return FactoriaObjetosNegocio.getInstance().getCargaBO();
    }

    @Override
    boolean esNuevo(Carga entidad) {
        return (entidad.getId() == null);
    }

    @Override
    String getId(Carga entidad) {
        return entidad.getId().toString();
    }

    @Override
    Integer convertirId(String cadena) throws Exception {
        return convertirIdAEntero(cadena);
    }

    @Override
    void mostrarLista(List<Carga> lista) {
        int i = 5;
        for (Carga c : lista) {
            setValorCelda(i, 1, c.getId());
            setValorCelda(i, 2, c.getId());
            if (c.isEsRama()){
                setValorCelda(i, 2, "Si");
            }else{
                setValorCelda(i, 2, "No");
            }
            i++;
        }

    }

    @Override
    void mostrarEntidad(Carga entidad) {

        setValorCelda(3, 2, entidad.getId());
        setValorCelda(4, 2, entidad.getCodigo());
        if (entidad.isEsRama()) {
            setValorCelda(5, 2, "Si");
        } else {
            setValorCelda(5, 2, "No");
        }

    }

}
