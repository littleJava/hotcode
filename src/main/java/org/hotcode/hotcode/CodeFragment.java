package org.hotcode.hotcode;

import java.lang.reflect.Modifier;

import org.hotcode.hotcode.constant.HotCodeConstant;
import org.hotcode.hotcode.reloader.CRMManager;
import org.hotcode.hotcode.reloader.ClassReloader;
import org.hotcode.hotcode.reloader.ClassReloaderManager;
import org.hotcode.hotcode.structure.FieldsHolder;
import org.hotcode.hotcode.util.HotCodeUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * Code fragment used to add to some place of a class.
 * 
 * @author khotyn 13-6-24 PM9:32
 */
public class CodeFragment {

    /**
     * Init static fields in method "<clinit>", should insert this code fragment at the start of "<clinit>".
     * 
     * @param mv
     */
    public static void clinitFieldInit(MethodVisitor mv, String ownerClassInternalName, Long classReloaderManagerIndex,
                                       Long classReloaderIndex) {
        mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(FieldsHolder.class));
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(FieldsHolder.class), "<init>",
                           Type.getMethodDescriptor(Type.VOID_TYPE));
        mv.visitFieldInsn(Opcodes.PUTSTATIC, ownerClassInternalName, HotCodeConstant.HOTCODE_STATIC_FIELDS,
                          Type.getDescriptor(FieldsHolder.class));

        mv.visitLdcInsn(classReloaderManagerIndex);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(CRMManager.class), "getClassReloaderManager",
                           Type.getMethodDescriptor(Type.getType(ClassReloaderManager.class), Type.LONG_TYPE));
        mv.visitLdcInsn(classReloaderIndex);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ClassReloaderManager.class), "getClassReloader",
                           Type.getMethodDescriptor(Type.getType(ClassReloader.class), Type.LONG_TYPE));
        mv.visitFieldInsn(Opcodes.PUTSTATIC, ownerClassInternalName, HotCodeConstant.HOTCODE_CLASS_RELOADER_FIELDS,
                          Type.getDescriptor(ClassReloader.class));
    }

    /**
     * Insert this code fragment at the start of a method, so it can check the update of a class file before every
     * method invoke.
     * 
     * @param mv
     * @param methodAccess
     * @param methodName
     * @param methodDesc
     * @param ownerClassInternalName
     */
    public static void beforeMethodCheck(MethodVisitor mv, int methodAccess, String methodName, String methodDesc,
                                         String ownerClassInternalName) {
        GeneratorAdapter ga = new GeneratorAdapter(mv, methodAccess, methodName, methodDesc);
        Label label = new Label();
        ga.visitFieldInsn(Opcodes.GETSTATIC, ownerClassInternalName, HotCodeConstant.HOTCODE_CLASS_RELOADER_FIELDS,
                          Type.getDescriptor(ClassReloader.class));
        ga.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ClassReloader.class), "checkAndReload",
                           Type.getMethodDescriptor(Type.BOOLEAN_TYPE));
        ga.visitInsn(Opcodes.ICONST_1);
        ga.visitJumpInsn(Opcodes.IF_ICMPNE, label);

        if (!Modifier.isStatic(methodAccess)) {
            ga.loadThis();
        }

        ga.loadArgs();
        ga.visitMethodInsn(Modifier.isStatic(methodAccess) ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL,
                           ownerClassInternalName, methodName, methodDesc);
        ga.returnValue();
        ga.visitLabel(label);
    }

    public static void checkReloadBeforeAccessField(MethodVisitor mv, String fieldOwner) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, fieldOwner, HotCodeConstant.HOTCODE_CLASS_RELOADER_FIELDS,
                          Type.getDescriptor(ClassReloader.class));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ClassReloader.class), "checkAndReload",
                           Type.getMethodDescriptor(Type.BOOLEAN_TYPE));
        mv.visitInsn(Opcodes.POP);
    }

    public static void initHotCodeInstanceFieldIfNull(MethodVisitor mv, String fieldOwner) {
        mv.visitInsn(Opcodes.DUP);
        mv.visitFieldInsn(Opcodes.GETFIELD, fieldOwner, HotCodeConstant.HOTCODE_INSTANCE_FIELDS,
                          Type.getDescriptor(FieldsHolder.class));
        Label label = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, label);
        mv.visitInsn(Opcodes.DUP);
        mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(FieldsHolder.class));
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(FieldsHolder.class), "<init>",
                           Type.getMethodDescriptor(Type.VOID_TYPE));
        mv.visitFieldInsn(Opcodes.PUTFIELD, fieldOwner, HotCodeConstant.HOTCODE_INSTANCE_FIELDS,
                          Type.getDescriptor(FieldsHolder.class));
        mv.visitLabel(label);
    }

    /**
     * Pack arguments to a array and return the local index of the array.
     * 
     * @param ga
     * @param desc
     * @return
     */
    public static int packArgsToArray(GeneratorAdapter ga, String desc) {
        Type[] argumentTypes = Type.getArgumentTypes(desc);
        ga.push(argumentTypes.length);
        ga.newArray(Type.getType(Object.class));
        int localIndex = ga.newLocal(Type.getType(Object[].class));
        ga.storeLocal(localIndex);

        for (int i = 0; i < argumentTypes.length; i++) {
            ga.box(argumentTypes[i]);
            ga.loadLocal(localIndex);
            ga.swap();
            ga.push(i);
            ga.swap();
            ga.arrayStore(HotCodeUtil.getBoxedType(argumentTypes[i]));
        }

        return localIndex;
    }
}
