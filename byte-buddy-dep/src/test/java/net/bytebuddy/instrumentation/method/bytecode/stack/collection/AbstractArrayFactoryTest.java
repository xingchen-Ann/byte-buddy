package net.bytebuddy.instrumentation.method.bytecode.stack.collection;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public abstract class AbstractArrayFactoryTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription componentTypeDescription;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    protected void testCreationUsing(Class<?> componentType, int storageOpcode) throws Exception {
        defineComponentType(componentType);
        CollectionFactory arrayFactory = ArrayFactory.targeting(componentTypeDescription);
        StackManipulation arrayStackManipulation = arrayFactory.withValues(Arrays.asList(stackManipulation));
        assertThat(arrayStackManipulation.isValid(), is(true));
        verify(stackManipulation, atLeast(1)).isValid();
        StackManipulation.Size size = arrayStackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(3 + StackSize.of(componentType).toIncreasingSize().getSizeImpact()));
        verify(methodVisitor).visitInsn(Opcodes.ICONST_1);
        verifyArrayCreation(methodVisitor);
        verify(methodVisitor).visitInsn(Opcodes.DUP);
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);
        verify(stackManipulation).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitInsn(storageOpcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(stackManipulation);
    }

    protected abstract void verifyArrayCreation(MethodVisitor methodVisitor);

    private void defineComponentType(Class<?> componentType) {
        when(componentTypeDescription.isPrimitive()).thenReturn(componentType.isPrimitive());
        when(componentTypeDescription.represents(componentType)).thenReturn(true);
        when(componentTypeDescription.getInternalName()).thenReturn(Type.getInternalName(componentType));
        when(componentTypeDescription.getStackSize()).thenReturn(StackSize.of(componentType));
        when(stackManipulation.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(StackSize.of(componentType).toIncreasingSize());
    }
}
