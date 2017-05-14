package com.damianogiusti.okrxwebsocket.exceptions;

/**
 * Created by Damiano Giusti on 14/05/17.
 */
public class ParserNotImplementedException extends RuntimeException {

    public ParserNotImplementedException() {
        super("Unable to parse socket response without a parser");
    }
}
