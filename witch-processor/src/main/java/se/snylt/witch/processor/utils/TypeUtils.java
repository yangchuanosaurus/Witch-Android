package se.snylt.witch.processor.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import se.snylt.witch.annotations.BindData;
import se.snylt.witch.processor.WitchException;

import static se.snylt.witch.processor.utils.ProcessorUtils.isAccessibleMethod;

public class TypeUtils {

    private final Types types;

    private final Elements elements;

    public static final TypeName STRING = ClassName.bestGuess("String");

    public static final TypeName HASH_MAP_STRINGS = ParameterizedTypeName.get(ClassName.get("java.util","HashMap"), STRING, STRING);

    private final static String LIBRARY_PACKAGE = "se.snylt.witchcore";

    private final static String LIBRARY_VIEW_BINDER_PACKAGE = "se.snylt.witchcore.viewbinder";

    private final static String LIBRARY_BIND_ACTIONS_PACKAGE = "se.snylt.witchcore.bindaction";

    public static final ClassName PRINTER = ClassName.get(LIBRARY_PACKAGE, "TargetPrinter");

    public static final ClassName LIST = ClassName.get(List.class);

    public static final TypeName ARRAY_LIST = TypeName.get(ArrayList.class);

    public final static ClassName VIEW_BINDER = ClassName.get(LIBRARY_VIEW_BINDER_PACKAGE, "ViewBinder");

    public final static TypeName VALUE = ClassName.get(LIBRARY_PACKAGE, "Value");

    public final static TypeName DIFF_VALUE = ClassName.get(LIBRARY_VIEW_BINDER_PACKAGE, "DiffValue");

    public static final ClassName TARGET_VIEW_BINDER = ClassName.get(LIBRARY_PACKAGE, "TargetViewBinder");

    public final static TypeName TARGET_VIEW_BINDER_FACTORY = ClassName.get(LIBRARY_PACKAGE, "TargetViewBinderFactory");

    public final static TypeName BINDER = ClassName.get(LIBRARY_BIND_ACTIONS_PACKAGE, "Binder");

    public static final TypeName ANDROID_VIEW = ClassName.get("android.view", "View");

    public static final ClassName ON_BIND = ClassName.get(LIBRARY_BIND_ACTIONS_PACKAGE, "OnBind");

    public static final ClassName SYNC_ON_BIND = ClassName.get(LIBRARY_BIND_ACTIONS_PACKAGE, "SyncOnBind");

    public TypeUtils(Types types, Elements elements) {
        this.types = types;
        this.elements = elements;
    }

    public Types types() {
        return types;
    }

    private String asString(ClassName className) {
        return className.packageName() + "." + className.simpleName();
    }

    private TypeMirror declared(ClassName name) {
        return types.getDeclaredType(elements.getTypeElement(asString(name)),
                types.getWildcardType(null, null),
                types.getWildcardType(null, null));
    }

    TypeMirror typeMirror(TypeName typeName) {
        return types.getDeclaredType(elements.getTypeElement(typeName.toString()));
    }

    boolean isValueContainer(TypeMirror type) {
        TypeMirror valueType = typeMirror(VALUE);
        return types.isAssignable(type, valueType);
    }

    public boolean isValueContainer(Element element) {
        return isValueContainer(getType(element));
    }

    public TypeMirror getType(Element element) {
        if (element.getKind().isField()) {
            return element.asType();
        } else if (element.getKind() == ElementKind.METHOD) {
            ExecutableType ext = (ExecutableType) element.asType();
            return ext.getReturnType();
        }
        return null;
    }

    public TypeName getValueType(Element value) {

        TypeMirror type = getType(value);

        type = boxed(type);

        return TypeName.get(type);
    }

    private TypeMirror boxed(TypeMirror type) {

        // Box if primitive
        if(type.getKind() != null && type.getKind().isPrimitive()) {
            return types.boxedClass((PrimitiveType) type).asType();
        }
        return type;
    }

    public TypeName[] getBindMethodTypeNames(Element bindMethod) throws WitchException {

        if(!isAccessibleMethod(bindMethod)) {
            throw WitchException.bindMethodNotAccessible(bindMethod);
        }

        ExecutableType type = (ExecutableType) bindMethod.asType();
        List<? extends TypeMirror> parameters = type.getParameterTypes();
        if(parameters.size() != 2) {
            throw WitchException.bindMethodWrongArgumentCount(bindMethod);
        }

        // View
        TypeMirror view = parameters.get(0);
        if(!types.isSubtype(view, typeMirror(ANDROID_VIEW))) {
            throw WitchException.bindMethodWrongViewType(bindMethod);
        }

        // Data
        TypeMirror data = boxed(parameters.get(1));

        return new TypeName[]{TypeName.get(view), TypeName.get(data)};

    }

    public TypeName getPropertyMethodViewType(Element element){
        if (element.getKind() == ElementKind.METHOD ) {
            ExecutableType type = (ExecutableType) element.asType();
            return TypeName.get(boxed(type.getParameterTypes().get(0)));
        }

        throw new IllegalArgumentException("Element is not a method");
    }

    public TypeName getPropertyMethodValueType(Element element) {
        if (element.getKind() == ElementKind.METHOD) {
            ExecutableType type = (ExecutableType) element.asType();
            return TypeName.get(boxed(type.getParameterTypes().get(1)));
        }
        throw new IllegalArgumentException("Element is not a method");
    }

    public static TypeName getBindDataViewType(Element action) {
        TypeMirror bindClass;
        try {
            action.getAnnotation(BindData.class).view();
            return null;
        } catch (MirroredTypeException mte) {
            bindClass = mte.getTypeMirror();
        }
        return TypeName.get(bindClass);
    }

}
