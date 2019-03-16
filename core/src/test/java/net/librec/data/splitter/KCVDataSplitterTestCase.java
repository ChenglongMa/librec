/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.data.splitter;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configured;
import net.librec.data.convertor.ArffDataConvertor;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * KCVDataSplitter TestCase {@link net.librec.data.splitter.KCVDataSplitter}
 *
 * @author Liuxz and Sunyt
 */
public class KCVDataSplitterTestCase extends BaseTestCase {

    private TextDataConvertor convertor;
    private TextDataConvertor convertorWithDate;
    private TextDataConvertor textDataConvertor;
    private ArffDataConvertor arffDataConvertor;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/matrix4by4A.txt");
        convertor = new TextDataConvertor(
                new String[]{"user", "item", "rating"},
                new String[]{"STRING", "STRING", "NUMERIC"},
                conf.get("inputDataPath"), " ");
        conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/matrix4by4A-date.txt");
        convertorWithDate = new TextDataConvertor(
                new String[]{"user", "item", "rating", "datetime"},
                new String[]{"STRING", "STRING", "NUMERIC", "NUMERIC"},
                conf.get("inputDataPath"), " ");
        conf.set("data.splitter.cv.number", "6");
    }

    /**
     * Test method splitData with dateMatrix
     *
     * @throws Exception
     */
    @Test
    public void testKCVWithoutDate() throws Exception {
        conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
        convertor.processData();
        KCVDataSplitter splitter = new KCVDataSplitter(convertor, conf);
        splitter.splitData();
        while (splitter.nextFold()) {
            assertEquals(splitter.getTrainData().size(), 10);
            assertEquals(splitter.getTestData().size(), 2);
        }
    }

    /**
     * Test method splitData without dateMatrix
     *
     * @throws Exception
     */
    @Test
    public void testKCVWithDate() throws Exception {
        conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
        convertorWithDate.processData();
        KCVDataSplitter splitter = new KCVDataSplitter(convertorWithDate, conf);
        splitter.splitData();
        while (splitter.nextFold()) {
            assertEquals(splitter.getTrainData().size(), 10);
            assertEquals(splitter.getTestData().size(), 2);
        }
    }

    private void buildConf() throws IOException {
        // text
        conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/ratings-date.txt");
        textDataConvertor = new TextDataConvertor(
                new String[]{"user", "item", "rating", "datetime"},
                new String[]{"STRING", "STRING", "NUMERIC", "NUMERIC"},
                conf.get("inputDataPath"), "[\t;, ]");
        conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/ratings.arff");
        conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
        textDataConvertor.processData();

        // arff
        arffDataConvertor = new ArffDataConvertor(conf.get("inputDataPath"));
        arffDataConvertor.processData();

        // base
        conf.set("data.splitter.cv.number", "6");
        conf.setInt("rec.random.seed", 1);
    }

    @Test
    public void testKCVTextVsARFF() throws IOException, LibrecException {
        buildConf();
        KCVDataSplitter textSplitter = new KCVDataSplitter(textDataConvertor, conf);
        textSplitter.splitData();
        KCVDataSplitter arffSplitter = new KCVDataSplitter(arffDataConvertor, conf);
        arffSplitter.splitData();
        //
        SequentialAccessSparseMatrix text, arff;
        List<Double> textVals = new ArrayList<>(), arffVals = new ArrayList<>();
        List<SequentialSparseVector> textRowVals = new ArrayList<>(), arffRowVals = new ArrayList<>();
        while (textSplitter.nextFold()) {
            text = textSplitter.getTrainData();
            textRowVals.add(text.row(2));
            double tVal = text.get(2, 2);
            textVals.add(tVal);
        }
        while (arffSplitter.nextFold()) {
            arff = arffSplitter.getTrainData();
            arffRowVals.add(arff.row(2));
            double aVal = arff.get(2, 2);
            arffVals.add(aVal);
        }
        for (int i = 0; i < textRowVals.size(); i++) {
            SequentialSparseVector tVal = textRowVals.get(i);
            SequentialSparseVector aVal = arffRowVals.get(i);
            assertArrayEquals(tVal.getIndices(), aVal.getIndices());
        }
    }
}
