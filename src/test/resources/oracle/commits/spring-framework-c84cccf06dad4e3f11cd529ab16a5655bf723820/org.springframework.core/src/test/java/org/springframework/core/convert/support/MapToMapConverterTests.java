package org.springframework.core.convert.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

public class MapToMapConverterTests {

	private GenericConversionService conversionService = new GenericConversionService();

	@Before
	public void setUp() {
		conversionService.addConverter(new MapToMapConverter(conversionService));
	}

	@Test
	public void scalarMap() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("1", "9");
		map.put("2", "37");
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("scalarMapTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		try {
			conversionService.convert(map, sourceType, targetType);
		} catch (ConversionFailedException e) {
			assertTrue(e.getCause() instanceof ConverterNotFoundException);
		}
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")		
		Map<Integer, Integer> result = (Map<Integer, Integer>) conversionService.convert(map, sourceType, targetType);
		assertFalse(map.equals(result));
		assertEquals((Integer) 9, result.get(1));
		assertEquals((Integer) 37, result.get(2));
	}

	public Map<Integer, Integer> scalarMapTarget;

	@Test
	public void scalarMapNotGenericTarget() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("1", "9");
		map.put("2", "37");
		assertTrue(conversionService.canConvert(Map.class, Map.class));
		assertEquals(map, conversionService.convert(map, Map.class));
	}

	@Test
	public void scalarMapNotGenericSourceField() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("1", "9");
		map.put("2", "37");
		TypeDescriptor sourceType = new TypeDescriptor(getClass().getField("notGenericMapSource"));
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("scalarMapTarget"));		
		assertTrue(conversionService.canConvert(sourceType, targetType));
		try {
			conversionService.convert(map, sourceType, targetType);
		} catch (ConversionFailedException e) {
			assertTrue(e.getCause() instanceof ConverterNotFoundException);
		}
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")		
		Map<Integer, Integer> result = (Map<Integer, Integer>) conversionService.convert(map, sourceType, targetType);
		assertFalse(map.equals(result));
		assertEquals((Integer) 9, result.get(1));
		assertEquals((Integer) 37, result.get(2));		
	}
	
	public Map notGenericMapSource;
	
	@Test
	public void collectionMap() throws Exception {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("collectionMapTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		try {
			conversionService.convert(map, sourceType, targetType);
		} catch (ConversionFailedException e) {
			assertTrue(e.getCause() instanceof ConverterNotFoundException);
		}		
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")		
		Map<Integer, List<Integer>> result = (Map<Integer, List<Integer>>) conversionService.convert(map, sourceType, targetType);
		assertFalse(map.equals(result));
		assertEquals(Arrays.asList(9, 12), result.get(1));
		assertEquals(Arrays.asList(37, 23), result.get(2));
	}

	public Map<Integer, List<Integer>> collectionMapTarget;

	@Test
	public void collectionMapSourceTarget() throws Exception {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		TypeDescriptor sourceType = new TypeDescriptor(getClass().getField("sourceCollectionMapTarget"));
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("collectionMapTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		try {
			conversionService.convert(map, sourceType, targetType);
		} catch (ConversionFailedException e) {
			assertTrue(e.getCause() instanceof ConverterNotFoundException);
		}				
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")		
		Map<Integer, List<Integer>> result = (Map<Integer, List<Integer>>) conversionService.convert(map, sourceType, targetType);
		assertFalse(map.equals(result));
		assertEquals(Arrays.asList(9, 12), result.get(1));
		assertEquals(Arrays.asList(37, 23), result.get(2));
	}

	public Map<String, List<String>> sourceCollectionMapTarget;

	@Test
	public void collectionMapNotGenericTarget() throws Exception {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		assertTrue(conversionService.canConvert(Map.class, Map.class));
		assertEquals(map, conversionService.convert(map, Map.class));
	}

	@Test
	public void collectionMapNotGenericTargetCollectionToObjectInteraction() throws Exception {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		assertTrue(conversionService.canConvert(Map.class, Map.class));
		assertEquals(map, conversionService.convert(map, Map.class));
	}
	
	@Test
	public void emptyMap() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("emptyMapTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		assertEquals(map, conversionService.convert(map, sourceType, targetType));
	}

	public Map<String, String> emptyMapTarget;

	@Test
	public void emptyMapNoTargetGenericInfo() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		assertTrue(conversionService.canConvert(Map.class, Map.class));
		assertEquals(map, conversionService.convert(map, Map.class));
	}

	@Test
	public void emptyMapDifferentTargetImplType() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("emptyMapDifferentTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, String> result = (LinkedHashMap<String, String>) conversionService.convert(map, sourceType, targetType);
		assertEquals(map, result);
		assertEquals(LinkedHashMap.class, result.getClass());
	}

	public LinkedHashMap<String, String> emptyMapDifferentTarget;
	
}
