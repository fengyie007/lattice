package org.hiforce.lattice.runtime.ability.register;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Getter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.hifforce.lattice.annotation.model.*;
import org.hifforce.lattice.exception.LatticeRuntimeException;
import org.hifforce.lattice.model.ability.IBusinessExt;
import org.hifforce.lattice.model.business.BusinessTemplate;
import org.hifforce.lattice.model.business.IBusiness;
import org.hifforce.lattice.model.register.*;
import org.hifforce.lattice.model.scenario.ScenarioRequest;
import org.hifforce.lattice.utils.BizCodeUtils;
import org.hifforce.lattice.utils.BusinessExtUtils;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hifforce.lattice.utils.LatticeAnnotationUtils.*;

/**
 * @author Rocky Yu
 * @since 2022/9/18
 */
public class TemplateRegister {

    private static TemplateRegister instance;

    @Getter
    private final List<RealizationSpec> realizations = Lists.newArrayList();

    @Getter
    private final List<ProductSpec> products = Lists.newArrayList();

    @Getter
    private final List<UseCaseSpec> useCases = Lists.newArrayList();

    @Getter
    private final List<BusinessSpec> businesses = Lists.newArrayList();

    private TemplateRegister() {

    }

    public static TemplateRegister getInstance() {
        if (null == instance) {
            instance = new TemplateRegister();
        }
        return instance;
    }

    public BusinessTemplate getFirstMatchedBusiness(ScenarioRequest request) {
        return businesses.stream()
                .map(BusinessSpec::newInstance)
                .filter(p -> p.isEffect(request))
                .findFirst().orElse(null);
    }

    @SuppressWarnings("rawtypes")
    public List<BusinessSpec> registerBusinesses(Set<Class> classSet) {
        for (Class clz : classSet) {
            BusinessAnnotation annotation = getBusinessAnnotation(clz);
            if (null == annotation) {
                continue;
            }
            BusinessSpec businessSpec = new BusinessSpec();
            businessSpec.setBusinessClass(clz);
            businessSpec.setCode(annotation.getCode());
            businessSpec.setName(annotation.getName());
            businessSpec.setDescription(annotation.getDesc());
            businessSpec.setPriority(annotation.getPriority());
            businessSpec.getRealizations().addAll(realizations.stream()
                    .filter(p -> BizCodeUtils.isCodesMatched(p.getCode(), businessSpec.getCode()))
                    .collect(Collectors.toList()));
            businesses.add(businessSpec);
        }
        return businesses;
    }

    @SuppressWarnings("rawtypes")
    public List<UseCaseSpec> registerUseCases(Set<Class> classSet) {
        for (Class clz : classSet) {
            UseCaseAnnotation annotation = getUseCaseAnnotation(clz);
            if (null == annotation) {
                continue;
            }
            UseCaseSpec spec = new UseCaseSpec();
            spec.setUseCaseClass(clz);
            spec.setCode(annotation.getCode());
            spec.setName(annotation.getName());
            spec.setDescription(annotation.getDesc());
            spec.setPriority(annotation.getPriority());
            spec.setSdk(annotation.getSdk());

            spec.getRealizations().addAll(realizations.stream()
                    .filter(p -> StringUtils.equals(p.getCode(), spec.getCode()))
                    .collect(Collectors.toList()));

            spec.getExtensions().addAll(scanBusinessExtensions(annotation.getSdk()));
            useCases.add(spec);
        }
        useCases.sort(Comparator.comparingInt(UseCaseSpec::getPriority));
        return useCases;
    }

    @SuppressWarnings("all")
    private Set<ExtensionPointSpec> scanBusinessExtensions(Class<? extends IBusinessExt> businessExt) {
        Set<ExtensionPointSpec> extensionPointSpecList = Sets.newHashSet();

        Method[] methods = businessExt.getMethods();
        for (Method method : methods) {
            ExtensionAnnotation annotation = getExtensionAnnotation(method);
            if (null == annotation) {
                if (ClassUtils.isAssignable(method.getReturnType(), IBusiness.class)) {
                    extensionPointSpecList.addAll(scanBusinessExtensions(
                            (Class<? extends IBusinessExt>) method.getReturnType()));
                }
                continue;
            }
            ExtensionPointSpec extensionPointSpec = buildExtensionPointSpec(annotation, method);
            if (null != extensionPointSpec) {
                extensionPointSpecList.add(extensionPointSpec);
            }
        }
        return extensionPointSpecList;
    }

    private ExtensionPointSpec buildExtensionPointSpec(ExtensionAnnotation annotation, Method invokeMethod) {
        ExtensionPointSpec spec = new ExtensionPointSpec(invokeMethod);
        spec.setProtocolType(annotation.getProtocolType());
        spec.setCode(annotation.getCode());
        spec.setName(StringUtils.isEmpty(annotation.getName()) ? invokeMethod.getName() : annotation.getName());
        spec.setReduceType(annotation.getReduceType());
        spec.setDescription(annotation.getDesc());
        return spec;
    }

    @SuppressWarnings("rawtypes")
    public List<ProductSpec> registerProducts(Set<Class> classSet) {
        for (Class clz : classSet) {
            ProductAnnotation annotation = getProductAnnotation(clz);
            if (null == annotation) {
                continue;
            }
            ProductSpec productSpec = new ProductSpec();
            productSpec.setProductClass(clz);
            productSpec.setCode(annotation.getCode());
            productSpec.setName(annotation.getName());
            productSpec.setDescription(annotation.getDesc());
            productSpec.setPriority(annotation.getPriority());
            productSpec.getRealizations().addAll(realizations.stream()
                    .filter(p -> StringUtils.equals(p.getCode(), productSpec.getCode()))
                    .collect(Collectors.toList()));
            products.add(productSpec);
        }
        products.sort(Comparator.comparingInt(ProductSpec::getPriority));
        return products;
    }

    @SuppressWarnings("rawtypes")
    public List<RealizationSpec> registerRealizations(Set<Class> classSet) {
        for (Class clz : classSet) {
            RealizationAnnotation annotation = getRealizationAnnotation(clz);
            if (null == annotation) {
                continue;
            }
            for (String code : annotation.getCodes()) {
                RealizationSpec spec = new RealizationSpec();
                spec.setCode(code);
                spec.setScenario(annotation.getScenario());
                spec.setBusinessExtClass(annotation.getBusinessExtClass());
                try {
                    spec.setBusinessExt(annotation.getBusinessExtClass().newInstance());
                } catch (Exception e) {
                    throw new LatticeRuntimeException("LATTICE-CORE-RT-0005", clz.getName());
                }
                spec.getExtensionCodes().addAll(BusinessExtUtils.supportedExtCodes(spec.getBusinessExt()));
                realizations.add(spec);
            }
        }
        return realizations;
    }
}
