package com.leaselens.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.leaselens.data.csv.CsvFormatException;
import com.leaselens.data.csv.CsvParser;
import com.leaselens.data.csv.CsvTable;

class CsvParserTest {

    private static CsvTable parse(String text) throws Exception {
        return CsvParser.parse("test.csv", new StringReader(text));
    }

    @Test
    @DisplayName("Plain rows, trimming, and line numbers (header is line 1)")
    void plainRows() throws Exception {
        CsvTable t = parse("a, b ,c\n1,2,3\n 4 ,5,6\n");
        assertEquals(List.of("a", "b", "c"), t.header());
        assertEquals(List.of("4", "5", "6"), t.rows().get(1).fields());
        assertEquals(3, t.rows().get(1).lineNumber());
    }

    @Test
    @DisplayName("Quoted fields may contain commas, escaped quotes and line breaks")
    void quotedFields() throws Exception {
        CsvTable t = parse("id,name\n1,\"Cedar, \"\"Block A\"\"\"\n2,\"two\nlines\"\n3,x\n");
        assertEquals("Cedar, \"Block A\"", t.rows().get(0).fields().get(1));
        assertEquals("two\nlines", t.rows().get(1).fields().get(1));
        assertEquals(5, t.rows().get(2).lineNumber());
    }

    @Test
    @DisplayName("CRLF endings, a byte-order mark and blank lines are handled")
    void crlfBomAndBlankLines() throws Exception {
        CsvTable t = parse("﻿id,v\r\n\r\n1,2\r\n\n3,4");
        assertEquals(List.of("id", "v"), t.header());
        assertEquals(2, t.rows().size());
        assertEquals(List.of("3", "4"), t.rows().get(1).fields());
    }

    @Test
    @DisplayName("Empty values are kept as empty strings")
    void emptyValues() throws Exception {
        CsvTable t = parse("a,b,c\n1,,3\n,,\n");
        assertEquals(List.of("1", "", "3"), t.rows().get(0).fields());
        assertEquals(List.of("", "", ""), t.rows().get(1).fields());
    }

    @Test
    @DisplayName("An empty file gives an empty header and no rows")
    void emptyFile() throws Exception {
        CsvTable t = parse("");
        assertTrue(t.header().isEmpty());
        assertTrue(t.rows().isEmpty());
    }

    @Test
    @DisplayName("An unterminated quote is reported with its line number")
    void unterminatedQuote() {
        CsvFormatException e = assertThrows(CsvFormatException.class, () -> parse("a\n\"oops\n"));
        assertEquals(2, e.lineNumber());
    }
}
