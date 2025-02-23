package jmail;

import java.util.ArrayList;
import java.util.List;

public class EmailParsingContext {
	boolean atFound = false; // 是否找到 '@' 符號
    boolean inQuotes = false; // 是否在引號內
    boolean previousDot = false; // 前一個字符是否是 '.'
    boolean previousBackslash = false; // 前一個字符是否是 '\'
    boolean firstDomainChar = true; // 是否是域名的第一個字符
    boolean isIpAddress = false; // 是否是 IP 地址
    boolean requireAtOrDot = false; // 是否需要 '@' 或 '.'
    boolean requireAtDotOrComment = false; // 是否需要 '@'、'.' 或 '('
    boolean whitespace = false; // 是否在空白字符內
    boolean previousComment = false; // 前一個字符是否是注釋結束
    boolean requireAngledBracket = false; // 是否需要尖括號
    boolean containsWhiteSpace = false; // 是否包含空白字符
    boolean isAscii = true; // 是否是 ASCII 字符
    boolean removableQuotePair = true; // 引號對是否可移除
    boolean previousQuotedDot = false; // 引號內前一個字符是否是 '.'
    boolean requireQuotedAtOrDot = false; // 引號內是否需要 '@' 或 '.'

    StringBuilder localPart; // 本地部分
    StringBuilder localPartWithoutComments; // 沒有注釋的本地部分
    StringBuilder localPartWithoutQuotes; // 沒有引號的本地部分
    StringBuilder currentQuote; // 當前引號內的內容
    StringBuilder domain; // 域名部分
    StringBuilder domainWithoutComments; // 沒有注釋的域名部分
    StringBuilder currentDomainPart; // 當前域名部分
    List<String> domainParts = new ArrayList<>(); // 域名部分列表
    List<String> comments = new ArrayList<>(); // 注釋列表

    int localPartCommentLength = 0; // 本地部分注釋長度
    int domainCommentLength = 0; // 域名部分注釋長度
    int charactersOnLine = 1; // 行上的字符數

    EmailParsingContext(int size) {
        localPart = new StringBuilder(size);
        localPartWithoutComments = new StringBuilder(size);
        localPartWithoutQuotes = new StringBuilder(size);
        currentQuote = new StringBuilder();
        domain = new StringBuilder(size);
        domainWithoutComments = new StringBuilder(size);
        currentDomainPart = new StringBuilder();
    }
}
