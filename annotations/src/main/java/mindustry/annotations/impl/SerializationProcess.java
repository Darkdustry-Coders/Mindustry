package mindustry.annotations.impl;

import arc.struct.Seq;
// import mindurka.arcext.GenericMetadata;
import mindustry.annotations.BaseProcessor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.Set;

@SupportedAnnotationTypes("arc.mindurka.Serializable")
public class SerializationProcess extends BaseProcessor {
    interface Yeet<T> {
        T run() throws Throwable;
    }
    private <T> T yeet(Yeet<T> fn) {
        try {
            return fn.run();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private JavaFileObject loaderFile;
    private PrintWriter loaderWriter;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        Filer filer = processingEnv.getFiler();

        loaderFile = yeet(() -> filer.createSourceFile("_gen.serializers.LoadParsers"));
        loaderWriter = new PrintWriter(yeet(() -> loaderFile.openWriter()));

        loaderWriter.println("package _gen.serializers;");
        loaderWriter.println("public class LoadParsers {");
        loaderWriter.println("    private LoadParsers() {}");
        loaderWriter.println("    public static void loadAll() {");
        loaderWriter.println("        try {");
    }

    private String getClassOf(TypeMirror ty) {
        if (ty.getKind().isPrimitive()) {
            return ty.getKind().toString().toLowerCase();
        }

        switch (ty.getKind()) {
            case ARRAY: return getClassOf(((ArrayType) ty).getComponentType()) + "[]";
            case DECLARED: {
                TypeElement el = (TypeElement) ((DeclaredType) ty).asElement();
                if (el.getEnclosingElement().getKind() != ElementKind.CLASS)
                    return el.getQualifiedName().toString();
                Seq<TypeElement> nesting = new Seq<>();
                for (;;) {
                    nesting.add(el);
                    Element enclosing = el.getEnclosingElement();
                    if (enclosing.getKind() != ElementKind.CLASS) break;
                    el = (TypeElement) enclosing;
                }
                StringBuilder name = new StringBuilder(nesting.pop().getQualifiedName().toString());
                while (!nesting.isEmpty()) name.append(".").append(nesting.pop().getSimpleName());
                return name.toString();
            }

            default:
                return "#ERROR";
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment re) {
        if (re.processingOver()) {
            loaderWriter.println("            arc.util.Log.info(\"Class loading complete!\");");
            loaderWriter.println("        } catch (Exception e) {");
            loaderWriter.println("            throw new RuntimeException(\"Failed to load parsers\", e);");
            loaderWriter.println("        }");
            loaderWriter.println("    }");
            loaderWriter.println("}");
            loaderWriter.close();

            return false;
        }

        for (TypeElement annotation : annotations) {
            for (Element element : re.getElementsAnnotatedWith(annotation)) {
                TypeElement clazz = (TypeElement) element;
                String className = clazz.getQualifiedName().toString();

                loaderWriter.println("            {");
                if (clazz.getModifiers().contains(Modifier.STATIC))
                    loaderWriter.println("                Class<?> clazz = Class.forName(\""+className.substring(0, className.lastIndexOf('.'))+"$"+className.substring(className.lastIndexOf('.') + 1)+"\");");
                else
                    loaderWriter.println("                Class<?> clazz = Class.forName(\""+className+"\");");

                for (Element element$1 : clazz.getEnclosedElements()) {
                    if (!element$1.getModifiers().contains(Modifier.PUBLIC)) continue;

                    switch (element$1.getKind()) {
                        case CONSTRUCTOR: {
                            ExecutableElement exec = (ExecutableElement) element$1;
                            loaderWriter.print("                clazz.getConstructor(");
                            boolean first = true;
                            for (VariableElement param : exec.getParameters()) {
                                if (first) first = false;
                                else loaderWriter.print(", ");
                                loaderWriter.print(getClassOf(param.asType()) + ".class");
                            }
                            loaderWriter.println(");");
                        } break;
                        case FIELD: {
                            if (element$1.getModifiers().contains(Modifier.STATIC)) continue;
                            VariableElement field = (VariableElement) element$1;

                            loaderWriter.println("                clazz.getField(\""+field.getSimpleName()+"\");");
                        } break;
                    }
                }

                loaderWriter.println("            }");
            }
        }

        return false;
    }

    // private Filer filer;

    // private JavaFileObject loaderFile;
    // private PrintWriter loaderWriter;

    // private JavaFileObject cacheFile;
    // private PrintWriter cacheWriter;
    // private int lastCacheEntry = 0;
    // private final HashMap<String, Integer> cacheEntries = new HashMap<>(32);

    // interface Yeet<T> {
    //     T run() throws Throwable;
    // }
    // private <T> T yeet(Yeet<T> fn) {
    //     try {
    //         return fn.run();
    //     } catch (Throwable e) {
    //         throw new RuntimeException(e);
    //     }
    // }

    // @Override
    // public synchronized void init(ProcessingEnvironment processingEnv) {
    //     super.init(processingEnv);

    //     filer = processingEnv.getFiler();

    //     loaderFile = yeet(() -> filer.createSourceFile("_gen.serializers.LoadParsers"));
    //     loaderWriter = new PrintWriter(yeet(() -> loaderFile.openWriter()));

    //     loaderWriter.println("package _gen.serializers;");
    //     loaderWriter.println("public class LoadParsers {");
    //     loaderWriter.println("    private LoadParsers() {}");
    //     loaderWriter.println("    public static void loadAll() {");
    //     loaderWriter.println("        try {");

    //     cacheFile = yeet(() -> filer.createSourceFile("_gen.serializers.ParserCache"));
    //     cacheWriter = new PrintWriter(yeet(() -> cacheFile.openWriter()));

    //     cacheWriter.println("package _gen.serializers;");
    //     cacheWriter.println("import mindurka.arcext.GenericType;");
    //     cacheWriter.println("public class ParserCache {");
    //     cacheWriter.println("    private ParserCache() {}");
    // }

    // @Override
    // public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment re) {
    //     if (re.processingOver()) {
    //         loaderWriter.println("        } catch (Exception e) {");
    //         loaderWriter.println("            throw new RuntimeException(\"Failed to load parsers\", e);");
    //         loaderWriter.println("        }");
    //         loaderWriter.println("    }");
    //         loaderWriter.println("}");
    //         loaderWriter.close();

    //         loaderFile = null;
    //         loaderWriter = null;

    //         cacheWriter.println("}");
    //         cacheWriter.close();

    //         cacheFile = null;
    //         cacheWriter = null;

    //         return false;
    //     }

    //     for (TypeElement annotation : annotations) {
    //         Set<? extends Element> annotated = re.getElementsAnnotatedWith(annotation);

    //         for (Element element : annotated) {
    //             TypeElement clazz = (TypeElement) element;
    //             PrintWriter writer = new PrintWriter(yeet(() -> filer.createSourceFile("_gen.serializers." + clazz.getQualifiedName()).openWriter()));

    //             int prefix = clazz.getQualifiedName().toString().lastIndexOf(".");

    //             String packageName = (prefix == -1 ? "_gen.serializers" : "_gen.serializers." + clazz.getQualifiedName().toString().substring(0, prefix)).toLowerCase();

    //             loaderWriter.println("            Class.forName(\""+packageName+"."+clazz.getSimpleName()+"\");");

    //             writer.println("package " + packageName + ";");
    //             writer.println("public class"+clazz.getSimpleName()+" implements mindurka.arcext.Serializer<"+clazz.getQualifiedName()+"> {");
    //             writer.println("    private "+clazz.getSimpleName()+"() {}");
    //             writer.println("    public static final "+clazz.getSimpleName()+" INSTANCE = new "+clazz.getSimpleName()+"();");
    //             writer.println("    private static final "+clazz.getQualifiedName()+" DEFAULT = new "+clazz.getQualifiedName()+"();");
    //             writer.println("    private static boolean eq(Object a, Object b) { return (a == b) || (a == null ? false : a.equals(b)); }");
    //             writer.println("    @Override");
    //             writer.println("    public void serialize(mindurka.arcext.Serialize serialize, "+clazz.getQualifiedName()+" object) throws mindurka.arcext.SerializationException {");
    //             writer.println("        mindurka.arcext.SerializeAsObject s = serialize.asObject();");

    //             for (Element sub : clazz.getEnclosedElements()) {
    //                 try {
    //                     if (sub.getKind() != ElementKind.FIELD) continue;
    //                     if (sub.getModifiers().contains(Modifier.STATIC)) continue;
    //                     if (sub.getAnnotationMirrors().stream().anyMatch(x -> "mindurka.arcext.SkipSerialization".equals(((TypeElement) x.getAnnotationType().asElement()).getQualifiedName().toString()))) continue;
    //                     VariableElement el = (VariableElement) sub;
    //                     TypeMirror ty = sub.asType();

    //                     switch (ty.getKind()) {
    //                         case BOOLEAN:
    //                         case BYTE:
    //                         case CHAR:
    //                         case SHORT:
    //                         case INT:
    //                         case LONG:
    //                         case FLOAT:
    //                         case DOUBLE:
    //                             writer.println("        if (object."+sub.getSimpleName()+" != DEFAULT."+sub.getSimpleName()+") s.add(\""+sub.getSimpleName()+"\", object."+sub.getSimpleName()+");");
    //                             break;

    //                         case DECLARED: {
    //                             // TODO: Enums.
    //                             TypeElement superclazz = (TypeElement) ((DeclaredType) ty).asElement();

    //                             if (superclazz.getQualifiedName().toString().equals(String.class.getCanonicalName())) {
    //                                 writer.println("        if (!eq(object."+sub.getSimpleName()+", DEFAULT."+sub.getSimpleName()+")) s.add(\""+sub.getSimpleName()+"\", object."+sub.getSimpleName()+");");
    //                                 break;
    //                             }

    //                             if (superclazz.getInterfaces().stream().anyMatch(x ->
    //                                 ((TypeElement) ((DeclaredType) x).asElement()).getQualifiedName().toString().equals("mindurka.arcext.SerializeWith")
    //                             )) {
    //                                 writer.println("        if (!eq(object."+sub.getSimpleName()+", DEFAULT."+sub.getSimpleName()+")) s.add(\""+sub.getSimpleName()+"\", "+getGenericType(el)+", object."+sub.getSimpleName()+");");
    //                                 break;
    //                             }

    //                             if (superclazz.getAnnotationMirrors().stream().anyMatch(x -> "mindurka.arcext.Serializable".equals(((TypeElement) x.getAnnotationType().asElement()).getQualifiedName().toString()))) {
    //                                 prefix = superclazz.getQualifiedName().toString().lastIndexOf(".");
    //                                 writer.println("        if (!eq(object."+sub.getSimpleName()+", DEFAULT."+sub.getSimpleName()+")) s.add(\""+sub.getSimpleName()+"\", "+getGenericType(el)+", "+(prefix == -1 ? "_gen.serializers" : "_gen.serializers."+clazz.getQualifiedName().toString().substring(0, prefix)).toLowerCase()+"."+superclazz.getSimpleName()+".INSTANCE, object."+sub.getSimpleName()+");");
    //                                 break;
    //                             }

    //                             processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not handle type "+topLevelName(superclazz.asType())+"!");

    //                             break;
    //                         }

    //                         default:
    //                             processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Invalid field type!");
    //                             break;
    //                     }
    //                 } catch (Exception e) {
    //                     StringWriter w = new StringWriter();
    //                     PrintWriter p = new PrintWriter(w);
    //                     e.printStackTrace(p);
    //                     p.flush();
    //                     processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to process field "+sub+"! "+w);
    //                 }
    //             }

    //             writer.println("    }");

    //             writer.println("    @Override");
    //             writer.println("    public "+clazz.getQualifiedName()+" deserialize(mindurka.arcext.Deserialize deserialize) throws mindurka.arcext.SerializationException {");
    //             writer.println("        mindurka.arcext.DeserializeAsObject s = deserialize.asObject();");
    //             writer.println("        "+clazz.getQualifiedName()+" o = new "+clazz.getQualifiedName()+"();");

    //             for (Element sub : clazz.getEnclosedElements()) {
    //                 try {
    //                     if (sub.getKind() != ElementKind.FIELD) continue;
    //                     if (sub.getModifiers().contains(Modifier.STATIC)) continue;
    //                     if (sub.getAnnotationMirrors().stream().anyMatch(x -> "mindurka.arcext.SkipSerialization".equals(((TypeElement) x.getAnnotationType().asElement()).getQualifiedName().toString()))) continue;
    //                     VariableElement el = (VariableElement) sub;
    //                     TypeMirror ty = sub.asType();

    //                     switch (ty.getKind()) {
    //                         case BOOLEAN:
    //                         case BYTE:
    //                         case CHAR:
    //                         case SHORT:
    //                         case INT:
    //                         case LONG:
    //                         case FLOAT:
    //                         case DOUBLE:
    //                             writer.println("        if (object."+sub.getSimpleName()+" != DEFAULT."+sub.getSimpleName()+") s.add(\""+sub.getSimpleName()+"\", object."+sub.getSimpleName()+");");
    //                             break;

    //                         case DECLARED: {
    //                             // TODO: Enums.
    //                             TypeElement superclazz = (TypeElement) ((DeclaredType) ty).asElement();

    //                             if (superclazz.getQualifiedName().toString().equals(String.class.getCanonicalName())) {
    //                                 writer.println("        if (!eq(object."+sub.getSimpleName()+", DEFAULT."+sub.getSimpleName()+")) s.add(\""+sub.getSimpleName()+"\", object."+sub.getSimpleName()+");");
    //                                 break;
    //                             }

    //                             if (superclazz.getInterfaces().stream().anyMatch(x ->
    //                                 ((TypeElement) ((DeclaredType) x).asElement()).getQualifiedName().toString().equals("mindurka.arcext.SerializeWith")
    //                             )) {
    //                                 writer.println("        if (!eq(object."+sub.getSimpleName()+", DEFAULT."+sub.getSimpleName()+")) s.add(\""+sub.getSimpleName()+"\", "+getGenericType(el)+", object."+sub.getSimpleName()+");");
    //                                 break;
    //                             }

    //                             if (superclazz.getAnnotationMirrors().stream().anyMatch(x -> "mindurka.arcext.Serializable".equals(((TypeElement) x.getAnnotationType().asElement()).getQualifiedName().toString()))) {
    //                                 prefix = superclazz.getQualifiedName().toString().lastIndexOf(".");
    //                                 writer.println("        if (!eq(object."+sub.getSimpleName()+", DEFAULT."+sub.getSimpleName()+")) s.add(\""+sub.getSimpleName()+"\", "+getGenericType(el)+", "+(prefix == -1 ? "_gen.serializers" : "_gen.serializers."+clazz.getQualifiedName().toString().substring(0, prefix)).toLowerCase()+"."+superclazz.getSimpleName()+".INSTANCE, object."+sub.getSimpleName()+");");
    //                                 break;
    //                             }

    //                             processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not handle type "+topLevelName(superclazz.asType())+"!");

    //                             break;
    //                         }

    //                         default:
    //                             processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Invalid field type!");
    //                             break;
    //                     }
    //                 } catch (Exception e) {
    //                     StringWriter w = new StringWriter();
    //                     PrintWriter p = new PrintWriter(w);
    //                     e.printStackTrace(p);
    //                     p.flush();
    //                     processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to process field "+sub+"! "+w);
    //                 }
    //             }

    //             writer.println("        return o;");
    //             writer.println("    }");

    //             writer.println("}");

    //             writer.close();
    //         }
    //     }

    //     return false;
    // }

    // private String topLevelName(TypeMirror baseClass) {
    //     if (baseClass.getKind().isPrimitive())
    //         return baseClass.getKind().name().toLowerCase();

    //     switch (baseClass.getKind()) {
    //         case ARRAY:
    //             return topLevelName(((ArrayType) baseClass).getComponentType()) + "[]";
    //         case DECLARED:
    //             return ((TypeElement) ((DeclaredType) baseClass).asElement()).getQualifiedName().toString();
    //         case TYPEVAR: {
    //             TypeMirror ub = ((TypeVariable) baseClass).getUpperBound();
    //             if (ub instanceof DeclaredType) {
    //                 return ((TypeElement) ((DeclaredType) ub).asElement()).getQualifiedName().toString();
    //             }
    //             return "#UNKNOWN-TYPEVAR";
    //         }
    //         default:
    //             processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unsupported type kind "+baseClass.getKind().name());
    //             return "#ERROR";
    //     }
    // }

    // private String cacheEntryName(VariableElement baseClass) {
    //     AnnotationMirror metadata = baseClass.getAnnotationMirrors().stream().filter(x -> x.getAnnotationType().toString().equals(GenericMetadata.class.getName())).findAny().orElse(null);
    //     if (metadata != null) {
    //         try {
    //             // Behold, the annotation processing API.
    //             int[] remaining = new int[((List<? extends AnnotationValue>) metadata.getElementValues().entrySet().stream().filter(x -> x.getKey().getSimpleName().toString().equals("value")).findFirst().get().getValue().getValue()).stream().mapToInt(x -> processingEnv.getElementUtils().getTypeElement(x.getValue().toString()).getTypeParameters().size()).max().orElse(0) + 1];
    //             int remIdx = 1;
    //             int clsIdx = 1;
    //             TypeElement[] classes = ((List<? extends AnnotationValue>) metadata.getElementValues().entrySet().stream().filter(x -> x.getKey().getSimpleName().toString().equals("value")).findFirst().get().getValue().getValue()).stream().map(x -> processingEnv.getElementUtils().getTypeElement(x.getValue().toString())).toArray(TypeElement[]::new);

    //             remaining[0] = classes[0].getTypeParameters().size();
    //             if (remaining[0] == 0) return classes[0].getQualifiedName().toString();

    //             StringBuilder name = new StringBuilder(classes[0].getQualifiedName() + "<");

    //             do {
    //                 remIdx--;
    //                 while (remaining[remIdx] != 0) {
    //                     if (name.codePointAt(name.length() - 1) != '<') name.append(", ");
    //                     name.append(classes[clsIdx].getQualifiedName());

    //                     int thisIdx = remIdx;
    //                     remaining[thisIdx]--;

    //                     if (!classes[clsIdx].getTypeParameters().isEmpty()) {
    //                         name.append("<");
    //                         remIdx++;
    //                         remaining[remIdx] = classes[clsIdx].getTypeParameters().size();
    //                     }

    //                     clsIdx++;
    //                 }
    //             } while (remIdx != 0);

    //             return name + ">";
    //         } catch (Exception e) {
    //             StringWriter w = new StringWriter();
    //             PrintWriter p = new PrintWriter(w);
    //             e.printStackTrace(p);
    //             p.flush();
    //             processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to process '@GenericMetadata'! " + w);
    //             return "#ERROR";
    //         }
    //     }
    //     return cacheEntryName(baseClass.asType());
    // }
    // private String cacheEntryName(TypeMirror baseClass) {
    //     if (baseClass.getKind().isPrimitive())
    //         return baseClass.getKind().name().toLowerCase();

    //     switch (baseClass.getKind()) {
    //         case ARRAY:
    //             return cacheEntryName(((ArrayType) baseClass).getComponentType()) + "[]";
    //         case DECLARED: {
    //             TypeElement base = (TypeElement) ((DeclaredType) baseClass).asElement();
    //             StringBuilder name = new StringBuilder(base.getQualifiedName().toString());
    //             if (!base.getTypeParameters().isEmpty()) {
    //                 name.append("<");
    //                 boolean comma = false;
    //                 for (TypeParameterElement param : base.getTypeParameters()) {
    //                     if (comma) name.append(", ");
    //                     else comma = true;

    //                     name.append(cacheEntryName(param.asType()));
    //                 }
    //                 name.append(">");
    //             }
    //             return name.toString();
    //         }
    //         case TYPEVAR: {
    //             TypeVariable var = (TypeVariable) baseClass;
    //             if (var.getUpperBound() instanceof DeclaredType) {
    //                 DeclaredType upper = (DeclaredType) var.getUpperBound();
    //                 StringBuilder name = new StringBuilder(((TypeElement) upper.asElement()).getQualifiedName().toString());
    //                 if (!upper.getTypeArguments().isEmpty()) {
    //                     name.append("<");
    //                     boolean comma = false;
    //                     for (TypeMirror param : upper.getTypeArguments()) {
    //                         if (comma) name.append(", ");
    //                         else comma = true;

    //                         name.append(cacheEntryName(param));
    //                     }
    //                     name.append(">");
    //                 }
    //                 return name.toString();
    //             }
    //             return "#INVALID-TYPE";
    //         }
    //         default:
    //             processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unsupported type kind "+baseClass.getKind().name());
    //             return "#ERROR";
    //     }
    // }

    // private String getGenericType(VariableElement baseClass) {
    //     String name = cacheEntryName(baseClass);

    //     Integer entry = cacheEntries.get(name);
    //     if (entry == null) {
    //         AnnotationMirror metadata = baseClass.getAnnotationMirrors().stream().filter(x -> x.getAnnotationType().toString().equals(GenericMetadata.class.getName())).findAny().orElse(null);
    //         if (metadata != null) {
    //             try {
    //                 // Behold, the annotation processing API.
    //                 int[] remaining = new int[((List<? extends AnnotationValue>) metadata.getElementValues().entrySet().stream().filter(x -> x.getKey().getSimpleName().toString().equals("value")).findFirst().get().getValue().getValue()).stream().mapToInt(x -> processingEnv.getElementUtils().getTypeElement(x.getValue().toString()).getTypeParameters().size()).max().orElse(0) + 1];
    //                 boolean[] newGeneric = new boolean[remaining.length];
    //                 int remIdx = 1;
    //                 int clsIdx = 1;
    //                 TypeElement[] classes = ((List<? extends AnnotationValue>) metadata.getElementValues().entrySet().stream().filter(x -> x.getKey().getSimpleName().toString().equals("value")).findFirst().get().getValue().getValue()).stream().map(x -> processingEnv.getElementUtils().getTypeElement(x.getValue().toString())).toArray(TypeElement[]::new);

    //                 remaining[0] = classes[0].getTypeParameters().size();
    //                 if (remaining[0] == 0) return classes[0].getQualifiedName().toString();

    //                 StringBuilder gen = new StringBuilder("GenericType.of("+classes[0].getQualifiedName()+".class)");

    //                 do {
    //                     remIdx--;
    //                     while (remaining[remIdx] != 0) {
    //                         if (newGeneric[remIdx]) {
    //                             gen.append(", ");
    //                         }
    //                         int thisIdx = remIdx;
    //                         newGeneric[thisIdx] = true;

    //                         if (classes[clsIdx].getTypeParameters().isEmpty()) {
    //                             gen.append(".with(")
    //                                 .append(classes[clsIdx].getQualifiedName())
    //                                 .append(".class)");
    //                             remIdx++;
    //                             remaining[remIdx] = classes[clsIdx].getTypeParameters().size();
    //                             newGeneric[remIdx] = false;
    //                         } else {
    //                             gen.append(".with(GenericType.of(")
    //                                 .append(classes[clsIdx].getQualifiedName())
    //                                 .append(".class)");
    //                         }

    //                         remaining[thisIdx]--;
    //                         clsIdx++;
    //                     }
    //                     if (remIdx != 0) gen.append(")");
    //                 } while (remIdx != 0);

    //                 int newEntry = lastCacheEntry++;

    //                 cacheWriter.println("    public static GenericType<"+classes[0].getQualifiedName()+"> E"+newEntry+" = "+gen+".build();");
    //                 cacheEntries.put(name, newEntry);
    //                 return "_gen.serializers.ParsersCache.E"+newEntry;
    //             } catch (Exception e) {
    //                 StringWriter w = new StringWriter();
    //                 PrintWriter p = new PrintWriter(w);
    //                 e.printStackTrace(p);
    //                 p.flush();
    //                 processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to process '@GenericMetadata'! " + w);
    //                 return "#ERROR";
    //             }
    //         }
    //         return getGenericType(baseClass.asType());
    //     } else return "_gen.serializers.ParsersCache.E"+entry;
    // }
    // private String getGenericType(TypeMirror baseClass) {
    //     String name = cacheEntryName(baseClass);

    //     Integer entry = cacheEntries.get(name);
    //     if (entry == null) {
    //         if (baseClass.getKind().isPrimitive()) {
    //             int newEntry = lastCacheEntry++;
    //             cacheEntries.put(name, newEntry);
    //             cacheWriter.println("    public static GenericType<?> E"+newEntry+" = new GenericType<>("+baseClass.getKind().name().toLowerCase()+".class, new GenericType<?>[0]);");
    //             return "_gen.serializers.ParsersCache.E"+newEntry;
    //         }

    //         switch (baseClass.getKind()) {
    //             case ARRAY: {
    //                 String component = getGenericType(((ArrayType) baseClass).getComponentType());
    //                 int newEntry = lastCacheEntry++;
    //                 cacheEntries.put(name, newEntry);
    //                 cacheWriter.println("    public static GenericType<"+topLevelName(baseClass)+"[]> E"+newEntry+" = new GenericType<>("+topLevelName(baseClass)+"[].class, new GenericType<?>[] {"+component+"});");
    //                 return "_gen.serializers.ParsersCache.E"+newEntry;
    //             }
    //             case DECLARED: {
    //                 TypeElement base = (TypeElement) ((DeclaredType) baseClass).asElement();
    //                 StringBuilder generics = new StringBuilder();
    //                 if (base.getTypeParameters().isEmpty()) {
    //                     int newEntry = lastCacheEntry++;
    //                     cacheEntries.put(name, newEntry);
    //                     cacheWriter.println("    public static GenericType<"+topLevelName(baseClass)+"> E"+newEntry+" = new GenericType<>("+topLevelName(baseClass)+".class, new GenericType[0]);");
    //                     return "_gen.serializers.ParsersCache.E"+newEntry;
    //                 } else {
    //                     generics.append("{");
    //                     boolean comma = false;
    //                     for (TypeParameterElement param : base.getTypeParameters()) {
    //                         if (comma) generics.append(", ");
    //                         else comma = true;

    //                         generics.append(getGenericType(param.asType()));
    //                     }
    //                     generics.append("}");
    //                 }
    //                 int newEntry = lastCacheEntry++;
    //                 cacheEntries.put(name, newEntry);
    //                 cacheWriter.println("    public static GenericType<"+topLevelName(baseClass)+"> E"+newEntry+" = new GenericType<>("+topLevelName(baseClass)+".class, new GenericType<?>"+generics+");");
    //                 return "_gen.serializers.ParsersCache.E"+newEntry;
    //             }
    //             default:
    //                 processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unsupported type kind "+baseClass.getKind().name());
    //                 return "#ERROR";
    //         }
    //     } else return "_gen.serializers.ParsersCache.E"+entry;
    // }
}
