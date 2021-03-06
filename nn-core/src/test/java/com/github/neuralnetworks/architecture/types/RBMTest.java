package com.github.neuralnetworks.architecture.types;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.amd.aparapi.Kernel.EXECUTION_MODE;
import com.github.neuralnetworks.architecture.Layer;
import com.github.neuralnetworks.architecture.types.NNFactory;
import com.github.neuralnetworks.architecture.types.RBM;
import com.github.neuralnetworks.calculation.CalculationFactory;
import com.github.neuralnetworks.input.MultipleNeuronsOutputError;
import com.github.neuralnetworks.input.SimpleInputProvider;
import com.github.neuralnetworks.tensor.Matrix;
import com.github.neuralnetworks.tensor.TensorFactory;
import com.github.neuralnetworks.tensor.ValuesProvider;
import com.github.neuralnetworks.test.AbstractTest;
import com.github.neuralnetworks.training.TrainerFactory;
import com.github.neuralnetworks.training.TrainingInputProvider;
import com.github.neuralnetworks.training.events.LogTrainingListener;
import com.github.neuralnetworks.training.random.MersenneTwisterRandomInitializer;
import com.github.neuralnetworks.training.random.NNRandomInitializer;
import com.github.neuralnetworks.training.rbm.CDTrainerBase;
import com.github.neuralnetworks.util.Environment;
import com.github.neuralnetworks.util.RuntimeConfiguration;

@RunWith(Parameterized.class)
public class RBMTest extends AbstractTest
{
	public RBMTest(RuntimeConfiguration conf)
	{
		Environment.getInstance().setRuntimeConfiguration(conf);
	}

	@Parameters
	public static Collection<RuntimeConfiguration[]> runtimeConfigurations()
	{
		RuntimeConfiguration conf1 = new RuntimeConfiguration();
		conf1.getAparapiConfiguration().setExecutionMode(EXECUTION_MODE.SEQ);
		conf1.setUseDataSharedMemory(false);
		conf1.setUseWeightsSharedMemory(false);

		RuntimeConfiguration conf2 = new RuntimeConfiguration();
		conf2.getAparapiConfiguration().setExecutionMode(EXECUTION_MODE.SEQ);
		conf2.setUseDataSharedMemory(true);
		conf2.setUseWeightsSharedMemory(true);

		return Arrays.asList(new RuntimeConfiguration[][] { { conf1 }, { conf2 } });
	}

	/**
	 * Contrastive Divergence testing
	 */
	@Test
	@Ignore // LogTrainingListener needs to be fixed first
	public void testContrastiveDivergence()
	{
		Environment.getInstance().getRuntimeConfiguration().getAparapiConfiguration().setExecutionMode(EXECUTION_MODE.SEQ);

		// RBM with 6 visible, 2 hidden units and a bias
		Environment.getInstance().getRuntimeConfiguration().setUseWeightsSharedMemory(true);
		RBM rbm = NNFactory.rbm(6, 2, true);

		// We'll use a simple dataset of symptoms of a flu illness. There are 6
		// input features and the first three are symptoms of the illness - for
		// example 1 0 0 0 0 0 means that a patient has high temperature, 0 1
		// 0 0 0 0 - coughing, 1 1 0 0 0 0 - coughing and high temperature
		// and so on. The second three features are "counter" symptomps - when a
		// patient has one of those it is less likely that he's sick. For
		// example 0 0 0 1 0 0 means that he has a flu vaccine. It's possible
		// to have combinations between both - for exmample 0 1 0 1 0 0 means
		// that the patient is vaccinated, but he's also coughing. We will
		// consider a patient to be sick when he has at least two of the first
		// three and healthy if he has two of the second three
		TrainingInputProvider trainInputProvider = new SimpleInputProvider(new float[][] { { 1, 1, 1, 0, 0, 0 }, { 1, 0, 1, 0, 0, 0 }, { 1, 1, 0, 0, 0, 0 }, { 0, 1, 1, 0, 0, 0 }, { 0, 1, 1, 1, 0, 0 },
				{ 0, 0, 0, 1, 1, 1 }, { 0, 0, 1, 1, 1, 0 }, { 0, 0, 0, 1, 0, 1 }, { 0, 0, 0, 0, 1, 1 }, { 0, 0, 0, 1, 1, 0 } });
		TrainingInputProvider testInputProvider = new SimpleInputProvider(new float[][] { { 1, 1, 1, 0, 0, 0 }, { 1, 0, 1, 0, 0, 0 }, { 1, 1, 0, 0, 0, 0 }, { 0, 1, 1, 0, 0, 0 }, { 0, 1, 1, 1, 0, 0 },
				{ 0, 0, 0, 1, 1, 1 }, { 0, 0, 1, 1, 1, 0 }, { 0, 0, 0, 1, 0, 1 }, { 0, 0, 0, 0, 1, 1 }, { 0, 0, 0, 1, 1, 0 } }, new float[][] { { 1, 0 }, { 1, 0 }, { 1, 0 }, { 1, 0 }, { 1, 0 }, { 0, 1 },
				{ 0, 1 }, { 0, 1 }, { 0, 1 }, { 0, 1 } });
		MultipleNeuronsOutputError error = new MultipleNeuronsOutputError();

		// Contrastive divergence training
		CDTrainerBase t = TrainerFactory.cdSigmoidTrainer(rbm, trainInputProvider, testInputProvider, error, new NNRandomInitializer(new MersenneTwisterRandomInitializer(-0.01f, 0.01f)), 0.02f, 0.5f,
				0f, 0f, 1, 1, 150, false);

		// log data
		t.addEventListener(new LogTrainingListener(Thread.currentThread().getStackTrace()[1].getMethodName(), true, false));

		// training
		t.train();

		// testing
		t.test();

		assertEquals(0, t.getOutputError().getTotalNetworkError(), 0);
	}

	/**
	 * Persistent Contrastive Divergence testing
	 */
	@Test
	@Ignore // LogTrainingListener needs to be fixed first
	public void testPersistentContrastiveDivergence()
	{
		Environment.getInstance().getRuntimeConfiguration().getAparapiConfiguration().setExecutionMode(EXECUTION_MODE.SEQ);

		// RBM with 6 visible, 2 hidden units and bias
		Environment.getInstance().getRuntimeConfiguration().setUseWeightsSharedMemory(true);
		RBM rbm = NNFactory.rbm(6, 2, true);

		// We'll use a simple dataset of symptoms of a flu illness. There are 6
		// input features and the first three are symptoms of the illness - for
		// example 1 0 0 0 0 0 means that a patient has high temperature, 0 1
		// 0 0 0 0 - coughing, 1 1 0 0 0 0 - coughing and high temperature
		// and so on. The second three features are "counter" symptomps - when a
		// patient has one of those it is less likely that he's sick. For
		// example 0 0 0 1 0 0 means that he has a flu vaccine. It's possible
		// to have combinations between both - for exmample 0 1 0 1 0 0 means
		// that the patient is vaccinated, but he's also coughing. We will
		// consider a patient to be sick when he has at least two of the first
		// three and healthy if he has two of the second three
		TrainingInputProvider trainInputProvider = new SimpleInputProvider(new float[][] { { 1, 1, 1, 0, 0, 0 }, { 1, 0, 1, 0, 0, 0 }, { 1, 1, 0, 0, 0, 0 }, { 0, 1, 1, 0, 0, 0 }, { 0, 1, 1, 1, 0, 0 },
				{ 0, 0, 0, 1, 1, 1 }, { 0, 0, 1, 1, 1, 0 }, { 0, 0, 0, 1, 0, 1 }, { 0, 0, 0, 0, 1, 1 }, { 0, 0, 0, 1, 1, 0 } });
		TrainingInputProvider testInputProvider = new SimpleInputProvider(new float[][] { { 1, 1, 1, 0, 0, 0 }, { 1, 0, 1, 0, 0, 0 }, { 1, 1, 0, 0, 0, 0 }, { 0, 1, 1, 0, 0, 0 }, { 0, 1, 1, 1, 0, 0 },
				{ 0, 0, 0, 1, 1, 1 }, { 0, 0, 1, 1, 1, 0 }, { 0, 0, 0, 1, 0, 1 }, { 0, 0, 0, 0, 1, 1 }, { 0, 0, 0, 1, 1, 0 } }, new float[][] { { 1, 0 }, { 1, 0 }, { 1, 0 }, { 1, 0 }, { 1, 0 }, { 0, 1 },
				{ 0, 1 }, { 0, 1 }, { 0, 1 }, { 0, 1 } });
		MultipleNeuronsOutputError error = new MultipleNeuronsOutputError();

		// Persistent Contrastive divergence trainer
		CDTrainerBase t = TrainerFactory.cdSigmoidTrainer(rbm, trainInputProvider, testInputProvider, error, new NNRandomInitializer(new MersenneTwisterRandomInitializer(-0.01f, 0.01f)), 0.02f, 0.5f,
				0f, 0f, 1, 1, 100, true);

		// log data
		t.addEventListener(new LogTrainingListener(Thread.currentThread().getStackTrace()[1].getMethodName(), true, false));

		// training
		t.train();

		// testing
		t.test();

		assertEquals(0, t.getOutputError().getTotalNetworkError(), 0);
	}

	@Test
	public void testRBMLayerCalculator1()
	{
		RBM rbm = NNFactory.rbm(2, 2, false);
		rbm.setLayerCalculator(CalculationFactory.lcSigmoid(rbm, null));

		Matrix cg1 = rbm.getMainConnections().getWeights();
		cg1.set(0.1f, 0, 0);
		cg1.set(0.8f, 0, 1);
		cg1.set(0.4f, 1, 0);
		cg1.set(0.6f, 1, 1);


		ValuesProvider vp = TensorFactory.tensorProvider(rbm, 1, Environment.getInstance().getRuntimeConfiguration().getUseDataSharedMemory());
		Matrix visible = vp.get(rbm.getVisibleLayer());
		visible.set(0.35f, 0, 0);
		visible.set(0.9f, 0, 1);

		Set<Layer> calculated = new HashSet<Layer>();
		calculated.add(rbm.getVisibleLayer());
		rbm.getLayerCalculator().calculate(rbm, rbm.getHiddenLayer(), calculated, vp);

		Matrix hidden = vp.get(rbm.getHiddenLayer());
		assertEquals(0.68, hidden.get(0, 0), 0.01);
		assertEquals(0.6637, hidden.get(0, 1), 0.01);
	}

	@Test
	public void testRBMLayerCalculator2()
	{
		RBM rbm = NNFactory.rbm(2, 2, false);
		rbm.setLayerCalculator(CalculationFactory.lcSigmoid(rbm, null));

		Matrix cg1 = rbm.getMainConnections().getWeights();
		cg1.set(0.1f, 0, 0);
		cg1.set(0.8f, 1, 0);
		cg1.set(0.4f, 0, 1);
		cg1.set(0.6f, 1, 1);

		ValuesProvider vp = TensorFactory.tensorProvider(rbm, 1, Environment.getInstance().getRuntimeConfiguration().getUseDataSharedMemory());
		Matrix hidden = vp.get(rbm.getHiddenLayer());
		hidden.set(0.35f, 0, 0);
		hidden.set(0.9f, 0, 1);

		Set<Layer> calculated = new HashSet<Layer>();
		calculated.add(rbm.getHiddenLayer());
		rbm.getLayerCalculator().calculate(rbm, rbm.getVisibleLayer(), calculated, vp);

		Matrix visible = vp.get(rbm.getVisibleLayer());
		assertEquals(0.68, visible.get(0, 0), 0.01);
		assertEquals(0.6637, visible.get(0, 1), 0.01);
	}

	@Test
	public void testRBMLayerCalculator3()
	{
		RBM rbm = NNFactory.rbm(3, 2, true);
		rbm.setLayerCalculator(CalculationFactory.lcSigmoid(rbm, null));

		Matrix cg1 = rbm.getMainConnections().getWeights();
		cg1.set(0.2f, 0, 0);
		cg1.set(0.4f, 0, 1);
		cg1.set(-0.5f, 0, 2);
		cg1.set(-0.3f, 1, 0);
		cg1.set(0.1f, 1, 1);
		cg1.set(0.2f, 1, 2);

		Matrix cgb1 = rbm.getHiddenBiasConnections().getWeights();
		cgb1.set(-0.4f, 0, 0);
		cgb1.set(0.2f, 1, 0);

		ValuesProvider vp = TensorFactory.tensorProvider(rbm, 1, Environment.getInstance().getRuntimeConfiguration().getUseDataSharedMemory());
		Matrix visible = vp.get(rbm.getVisibleLayer());
		visible.set(1f, 0, 0);
		visible.set(0f, 0, 1);
		visible.set(1f, 0, 2);

		Matrix hiddenBias = vp.get(rbm.getHiddenBiasConnections().getInputLayer());
		hiddenBias.set(1, 0);

		Set<Layer> calculated = new HashSet<Layer>();
		calculated.add(rbm.getVisibleLayer());
		rbm.getLayerCalculator().calculate(rbm, rbm.getHiddenLayer(), calculated, vp);

		Matrix hidden = vp.get(rbm.getHiddenLayer());
		assertEquals(0.332, hidden.get(0, 0), 0.001);
		assertEquals(0.525, hidden.get(0, 1), 0.001);
	}

	@Test
	public void testRBMLayerCalculator4()
	{
		RBM rbm = NNFactory.rbm(2, 3, true);
		rbm.setLayerCalculator(CalculationFactory.lcSigmoid(rbm, null));

		Matrix cg1 = rbm.getMainConnections().getWeights();
		cg1.set(0.2f, 0, 0);
		cg1.set(0.4f, 1, 0);
		cg1.set(-0.5f, 2, 0);
		cg1.set(-0.3f, 0, 1);
		cg1.set(0.1f, 1, 1);
		cg1.set(0.2f, 2, 1);

		Matrix cgb1 = rbm.getVisibleBiasConnections().getWeights();
		cgb1.set(-0.4f, 0, 0);
		cgb1.set(0.2f, 1, 0);

		ValuesProvider vp = TensorFactory.tensorProvider(rbm, 1, Environment.getInstance().getRuntimeConfiguration().getUseDataSharedMemory());
		Matrix hidden = vp.get(rbm.getHiddenLayer());
		hidden.set(1f, 0, 0);
		hidden.set(0f, 0, 1);
		hidden.set(1f, 0, 2);

		Set<Layer> calculated = new HashSet<Layer>();
		calculated.add(rbm.getHiddenLayer());
		rbm.getLayerCalculator().calculate(rbm, rbm.getVisibleLayer(), calculated, vp);

		Matrix visible = vp.get(rbm.getVisibleLayer());
		assertEquals(0.332, visible.get(0, 0), 0.001);
		assertEquals(0.525, visible.get(0, 1), 0.001);
	}

	@Test
	public void testOneStepContrastiveDivergence()
	{
		RBM rbm = NNFactory.rbm(3, 2, true);

		Matrix cg1 = rbm.getMainConnections().getWeights();
		cg1.set(0.2f, 0, 0);
		cg1.set(0.4f, 0, 1);
		cg1.set(-0.5f, 0, 2);
		cg1.set(-0.3f, 1, 0);
		cg1.set(0.1f, 1, 1);
		cg1.set(0.2f, 1, 2);

		Matrix cgb1 = rbm.getVisibleBiasConnections().getWeights();
		cgb1.set(0f, 0, 0);
		cgb1.set(0f, 1, 0);
		cgb1.set(0f, 2, 0);

		Matrix cgb2 = rbm.getHiddenBiasConnections().getWeights();
		cgb2.set(-0.4f, 0, 0);
		cgb2.set(0.2f, 1, 0);

		CDTrainerBase t = TrainerFactory.cdSigmoidTrainer(rbm, new SimpleInputProvider(new float[][] { { 1, 0, 1 } }), null, null, null, 1f, 0f, 0f, 0f, 1, 1, 1, true);

		t.train();

		assertEquals(0.52276707, cgb1.get(0, 0), 0.00001);
		assertEquals(-0.54617375, cgb1.get(1, 0), 0.00001);
		assertEquals(0.51522285, cgb1.get(2, 0), 0.00001);

		assertEquals(-0.4 - 0.08680013, cgb2.get(0, 0), 0.00001);
		assertEquals(0.2 - 0.02693379, cgb2.get(1, 0), 0.00001);

		assertEquals(0.2 + 0.13203661, cg1.get(0, 0), 0.00001);
		assertEquals(0.4 - 0.22863509, cg1.get(0, 1), 0.00001);
		assertEquals(-0.5 + 0.12887852, cg1.get(0, 2), 0.00001);
		assertEquals(-0.3 + 0.26158813, cg1.get(1, 0), 0.00001);
		assertEquals(0.1 - 0.3014404, cg1.get(1, 1), 0.00001);
		assertEquals(0.2 + 0.25742438, cg1.get(1, 2), 0.00001);
	}

	@Test
	public void testTwoStepContrastiveDivergence()
	{
		RBM rbm = NNFactory.rbm(3, 2, true);

		Matrix cg1 = rbm.getMainConnections().getWeights();
		cg1.set(0.2f, 0, 0);
		cg1.set(0.4f, 0, 1);
		cg1.set(-0.5f, 0, 2);
		cg1.set(-0.3f, 1, 0);
		cg1.set(0.1f, 1, 1);
		cg1.set(0.2f, 1, 2);

		Matrix cgb1 = rbm.getVisibleBiasConnections().getWeights();
		cgb1.set(0f, 0, 0);
		cgb1.set(0f, 1, 0);
		cgb1.set(0f, 2, 0);

		Matrix cgb2 = rbm.getHiddenBiasConnections().getWeights();
		cgb2.set(-0.4f, 0, 0);
		cgb2.set(0.2f, 1, 0);

		CDTrainerBase t = TrainerFactory.cdSigmoidTrainer(rbm, new SimpleInputProvider(new float[][] { { 1, 0, 1 }, { 1, 1, 0 } }), null, null, null, 1f, 0f, 0f, 0f, 1, 1, 1, false);

		t.train();

		assertEquals(0.86090606, cgb1.get(0, 0), 0.00001);
		assertEquals(0.089616358, cgb1.get(1, 0), 0.00001);
		assertEquals(-0.11872697, cgb1.get(2, 0), 0.00001);

		assertEquals(-0.3744152, cgb2.get(0, 0), 0.00001);
		assertEquals(0.0663045, cgb2.get(1, 0), 0.00001);

		assertEquals(0.5768927, cg1.get(0, 0), 0.00001);
		assertEquals(0.5328304, cg1.get(0, 1), 0.00001);
		assertEquals(-0.619481, cg1.get(0, 2), 0.00001);
		assertEquals(0.0543526, cg1.get(1, 0), 0.00001);
		assertEquals(0.0669599, cg1.get(1, 1), 0.00001);
		assertEquals(0.0833487, cg1.get(1, 2), 0.00001);
	}
}
