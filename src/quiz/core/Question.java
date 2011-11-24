/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.core;

import java.util.List;

/**
 *
 * @author higor
 */
public class Question {
    private String statement;
    private List<String> answers;
    private int correct;

    public Question(String statement) {
        setStatement(statement);
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }
    
    public String getAnswer(int n) {
        return answers.get(n);
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> alternative) {
        this.answers = alternative;
    }

    public int getCorrect() {
        return correct;
    }

    public void setCorrect(int correct) {
        this.correct = correct;
    }

}
