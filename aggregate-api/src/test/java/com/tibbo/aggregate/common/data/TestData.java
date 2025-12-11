package com.tibbo.aggregate.common.data;

import com.tibbo.aggregate.common.tests.*;

import java.nio.charset.*;

public class TestData extends CommonsTestCase
{
  public void testDataEncodingRestore() throws Exception
  {
    StringBuilder inputText = new StringBuilder();
    inputText.append(
        "Wikipedia (Listeni/ˌwɪkᵻˈpiːdiə/ or Listeni/ˌwɪkiˈpiːdiə/ wik-i-pee-dee-ə) is a free online encyclopedia that aims to allow anyone to edit articles.[3] Wikipedia is the largest and most popular general reference work on the Internet[4][5][6] and is ranked among the ten most popular websites.[7] Wikipedia is owned by the nonprofit Wikimedia Foundation.[8][9][10]");
    inputText.append(
        "Wikipedia was launched on January 15, 2001, by Jimmy Wales and Larry Sanger.[11] Sanger coined its name,[12][13] a portmanteau of wiki[notes 4] and encyclopedia. There was only the English language version initially, but it quickly developed similar versions in other languages, which differ in content and in editing practices. With 5,326,946 articles, the English");
    inputText.append(
        " Wikipedia is the largest of the more than 290 Wikipedia encyclopedias. Overall, Wikipedia consists of more than 40 million articles in more than 250 different languages[15] and as of February 2014, ");
    inputText.append("it had 18 billion page views and nearly 500 million unique visitors each month.[16]");
    inputText.append(
        "In 2005, Nature published a peer review comparing 42 science articles from Encyclopædia Britannica and Wikipedia, and found that Wikipedia's level of accuracy approached Encyclopædia Britannica's.[17] Criticism of Wikipedia includes claims that it exhibits systemic bias, presents a mixture of \"truths, half truths, and some falsehoods\",[18] and that in controversial topics, it is subject to manipulation and spin.[19]");
    
    Data data1 = new Data();
    data1.setData(inputText.toString().getBytes(Charset.forName("UTF-8")));
    StringBuilder encodeRes = data1.encode(new StringBuilder(), null, false, 0);
    
    Data data2 = new Data(encodeRes.toString());
    String outputText = new String(data2.getData(), Charset.forName("UTF-8"));
    
    assertEquals(inputText.toString(), outputText);
  }
  
  public void testEmptyData() throws Exception
  {
    String text = "0/\u001A/\u001A/-1/0/";
    Data data1 = new Data(text);
    
    String outputText = new String(data1.getData(), Charset.forName("UTF-8"));
    assertEquals("", outputText);
  }
}
