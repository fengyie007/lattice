package org.hifforce.lattice.annotation.processor;

import com.google.auto.service.AutoService;
import org.hifforce.lattice.annotation.Realization;
import org.hifforce.lattice.model.ability.IBusinessExt;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedOptions;
import java.lang.annotation.Annotation;

/**
 * @author Rocky Yu
 * @since 2022/9/15
 */
@AutoService(Processor.class)
@SupportedOptions({"debug", "verify"})
public class RealizationAnnotationProcessor extends LatticeAnnotationProcessor {

    public Class<?> getServiceInterfaceClass() {
        return IBusinessExt.class;
    }

    @Override
    public Class<? extends Annotation> getProcessAnnotationClass() {
        return Realization.class;
    }
}
