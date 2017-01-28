package se.snylt.zipper.processor;

import com.google.auto.service.AutoService;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import se.snylt.zipper.ClassUtils;
import se.snylt.zipper.annotations.BindTo;
import se.snylt.zipper.annotations.BindToCompoundButton;
import se.snylt.zipper.annotations.BindToEditText;
import se.snylt.zipper.annotations.BindToImageView;
import se.snylt.zipper.annotations.BindToRecyclerView;
import se.snylt.zipper.annotations.BindToTextView;
import se.snylt.zipper.annotations.BindToView;
import se.snylt.zipper.annotations.OnBind;

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "se.snylt.zipper.annotations.BindTo",
        "se.snylt.zipper.annotations.BindToView",
        "se.snylt.zipper.annotations.BindToTextView",
        "se.snylt.zipper.annotations.BindToEditText",
        "se.snylt.zipper.annotations.BindToImageView",
        "se.snylt.zipper.annotations.BindToCompoundButton",
        "se.snylt.zipper.annotations.BindToRecyclerView",
        "se.snylt.zipper.annotations.OnBind"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ZipperProcessor extends AbstractProcessor {

    private final static Class<? extends Annotation>[] BIND_VIEW_ANNOTATION = new Class[]{
            BindTo.class,
            BindToView.class,
            BindToTextView.class,
            BindToEditText.class,
            BindToImageView.class,
            BindToCompoundButton.class,
            BindToRecyclerView.class
    };

    private Types typeUtils;

    private Elements elementUtils;

    private Filer filer;

    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    private void log(String message){
        messager.printMessage(Diagnostic.Kind.NOTE, message);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        HashMap<Element, List<BindToViewActions>> targets = new HashMap<>();
        addTarget(targets, roundEnv, BIND_VIEW_ANNOTATION);
        addBindActions(targets, roundEnv);
        buildJava(targets);
        return true;
    }

    private void addTarget(Map<Element, List<BindToViewActions>> targets, RoundEnvironment roundEnv, Class<? extends Annotation> ...annotations) {
        for(Class<? extends Annotation> annotation: annotations) {
            for (Element value : roundEnv.getElementsAnnotatedWith(annotation)) {
                Element target = value.getEnclosingElement();

                // Prepare target bindings
                if (!targets.containsKey(target)) {
                    targets.put(target, new LinkedList<>());
                }

                // Add view id and field to be bound
                List<BindToViewActions> bindToViewActionses = targets.get(target);
                if (!bindToViewActionses.contains(value)) {
                    bindToViewActionses.add(new BindToViewActions(getViewId(value, annotation), value));
                }
            }
        }
    }

    private Integer getViewId(Element element, Class<? extends Annotation> annotation) {
        if(annotation == BindTo.class) {
            return element.getAnnotation(BindTo.class).value();
        } else if(annotation == BindToView.class) {
            return element.getAnnotation(BindToView.class).id();
        } else if(annotation == BindToTextView.class) {
            return element.getAnnotation(BindToTextView.class).id();
        } else if(annotation == BindToImageView.class) {
            return element.getAnnotation(BindToImageView.class).id();
        } else if(annotation == BindToEditText.class) {
            return element.getAnnotation(BindToEditText.class).id();
        } else if(annotation == BindToCompoundButton.class) {
            return element.getAnnotation(BindToCompoundButton.class).id();
        } else if(annotation == BindToRecyclerView.class) {
            return element.getAnnotation(BindToRecyclerView.class).id();
        }
        return null;
    }

    private void addBindActions(HashMap<Element, List<BindToViewActions>> binders, RoundEnvironment roundEnv) {
        // OnBind
        for (Element bindAction : roundEnv.getElementsAnnotatedWith(OnBind.class)) {
            TypeName onBindClass = getOnBindClass(bindAction);
            BindActionDef actionDef = new OnBindDef(onBindClass);
            addBindAction(bindAction, actionDef, binders);
        }

        // BindToView
        for (Element bindAction : roundEnv.getElementsAnnotatedWith(BindToView.class)) {
            String property = bindAction.getAnnotation(BindToView.class).set();
            TypeName viewType = getOnBindToViewClass(bindAction);
            addOnBindViewDef(binders, property, viewType, bindAction);
        }

        // BindToTextView
        for (Element bindAction : roundEnv.getElementsAnnotatedWith(BindToTextView.class)) {
            String property = bindAction.getAnnotation(BindToTextView.class).set();
            TypeName viewType  = ClassName.get("android.widget", "TextView");
            addOnBindViewDef(binders, property, viewType, bindAction);
        }

        // BindToImageView
        for (Element bindAction : roundEnv.getElementsAnnotatedWith(BindToImageView.class)) {
            String property = bindAction.getAnnotation(BindToImageView.class).set();
            TypeName viewType  = ClassName.get("android.widget", "ImageView");
            addOnBindViewDef(binders, property, viewType, bindAction);
        }

        // BindToEditText
        for (Element bindAction : roundEnv.getElementsAnnotatedWith(BindToEditText.class)) {
            String property = bindAction.getAnnotation(BindToEditText.class).set();
            TypeName viewType  = ClassName.get("android.widget", "EditText");
            addOnBindViewDef(binders, property, viewType, bindAction);
        }

        // BindToCompoundButton
        for (Element bindAction : roundEnv.getElementsAnnotatedWith(BindToCompoundButton.class)) {
            String property = bindAction.getAnnotation(BindToCompoundButton.class).set();
            TypeName viewType  = ClassName.get("android.widget", "CompoundButton");
            addOnBindViewDef(binders, property, viewType, bindAction);
        }

        // BindToRecyclerView
        for (Element bindAction : roundEnv.getElementsAnnotatedWith(BindToRecyclerView.class)) {
            String property = bindAction.getAnnotation(BindToRecyclerView.class).set();
            TypeName viewType  = ClassName.get("android.support.v7.widget", "RecyclerView");
            TypeName valueType  = ClassName.get(bindAction.asType());
            TypeName adapterType = getOnBindToRecyclerViewAdapterClass(bindAction);
            BindActionDef actionDef = new OnBindGetAdapterViewDef(property, viewType, adapterType, valueType);
            addBindAction(bindAction, actionDef, binders);
        }

        // TODO
        // BindToAdapterView
        // BindToProgressBar
        // BindToToggleButton
        // BindToCheckedTextView
        // BindToRatingBar
        // BindToTextSwitcher
        // BindToToolBar
    }

    private void addOnBindViewDef(HashMap<Element, List<BindToViewActions>> binders, String property, TypeName viewType, Element bindAction) {
        TypeName valueType  = ClassName.get(bindAction.asType());
        BindActionDef actionDef = new OnBindViewDef(property, viewType, valueType);
        addBindAction(bindAction, actionDef, binders);
    }

    private void addBindAction(Element bindAction, BindActionDef bindActionDef, HashMap<Element, List<BindToViewActions>> binders) {
        Element target = bindAction.getEnclosingElement();
        List<BindToViewActions> bindToViewActionses = binders.get(target);

        // Add bind actions to view binding
        for (BindToViewActions bindToViewActions : bindToViewActionses) {
            if (bindToViewActions.equals(bindAction)) {
                bindToViewActions.addBindAction(bindActionDef);
            }
        }
    }

    private void buildJava(HashMap<Element, List<BindToViewActions>> binders) {
        for (Element target : binders.keySet()) {

            // TODO refactor
            // View holder
            ClassName viewHolderClassName = getBindingViewHolderName(target);
            TypeSpec viewHolderTypeSpec = ViewHolderFactory.toJava(binders.get(target), viewHolderClassName);
            JavaFile viewHolderJavaFile = JavaFile.builder(viewHolderClassName.packageName(), viewHolderTypeSpec).build();
            try {
                viewHolderJavaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // View binder
            ClassName bindingClassName = getBindingClassName(target);
            ClassName targetClassName = getTargetClassName(target);
            TypeSpec bindingTypeSpec = BinderFactory.toJava(targetClassName, binders.get(target), bindingClassName, viewHolderClassName);
            JavaFile bindingJavaFile = JavaFile.builder(bindingClassName.packageName(), bindingTypeSpec).build();
            try {
                bindingJavaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ClassName getBindingViewHolderName(Element target) {
        String className = ClassUtils.getViewHolderName(target);
        String packageName = ClassUtils.getBindingPackage(target);
        return ClassName.get(packageName, className);
    }

    private ClassName getBindingClassName(Element target) {
        String className = ClassUtils.getBindingName(target);
        String packageName = ClassUtils.getBindingPackage(target);
        return ClassName.get(packageName, className);
    }

    private ClassName getTargetClassName(Element target) {
        String className = ClassUtils.getTargetName(target);
        String packageName = ClassUtils.getBindingPackage(target);
        return ClassName.get(packageName, className);
    }

    private TypeName getOnBindToRecyclerViewAdapterClass(Element bindAction) {
        TypeMirror bindClass = null;
        try {
            bindAction.getAnnotation(BindToRecyclerView.class).adapter();
        } catch (MirroredTypeException mte) {
            bindClass = mte.getTypeMirror();
        }
        return TypeName.get(bindClass);
    }

    private TypeName getOnBindClass(Element action){
        TypeMirror bindClass = null;
        try {
            action.getAnnotation(OnBind.class).value();
        } catch (MirroredTypeException mte) {
            bindClass = mte.getTypeMirror();
        }
        return TypeName.get(bindClass);
    }

    private TypeName getOnBindToViewClass(Element action) {
        TypeMirror bindClass = null;
        try {
            action.getAnnotation(BindToView.class).view();
        } catch (MirroredTypeException mte) {
            bindClass = mte.getTypeMirror();
        }
        return TypeName.get(bindClass);
    }
}
