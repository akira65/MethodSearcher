package method_searcher;

import java.util.ArrayList;
import java.util.List;


public class FailedMethods {
    private List<String> notFoundClasses;
    private List<String> notFoundMethods;
    private List<String> notFoundInputVariables;
    private List<String> notFoundOutputVariables;
    private List<String> isNotObjectInput;
    private List<String> isNotPrimitiveOutput;

    public FailedMethods() {
        this.notFoundClasses = new ArrayList<>();
        this.notFoundMethods = new ArrayList<>();
        this.notFoundInputVariables = new ArrayList<>();
        this.notFoundOutputVariables = new ArrayList<>();
        this.isNotObjectInput = new ArrayList<>();
        this.isNotPrimitiveOutput = new ArrayList<>();
    }

    // Getter and Setter methods for each list
    public List<String> getNotFoundClasses() {
        return notFoundClasses;
    }

    public void addNotFoundClasse(String notFoundClasses) {
        this.notFoundClasses.add(notFoundClasses);
    }

    public List<String> getNotFoundMethods() {
        return notFoundMethods;
    }

    public void addNotFoundMethod(String notFoundMethods) {
        this.notFoundMethods.add(notFoundMethods);
    }

    public List<String> getNotFoundInputVariables() {
        return notFoundInputVariables;
    }

    public void addNotFoundInputVariable(String notFoundInputVariables) {
        this.notFoundInputVariables.add(notFoundInputVariables);
    }

    public List<String> getNotFoundOutputVariables() {
        return notFoundOutputVariables;
    }

    public void addNotFoundOutputVariable(String notFoundOutputVariables) {
        this.notFoundOutputVariables.add(notFoundOutputVariables);
    }

    public List<String> getIsNotObjectInput() {
        return isNotObjectInput;
    }

    public void addIsNotObjectInput(String isNotObjectInput) {
        this.isNotObjectInput.add(isNotObjectInput);
    }

    public List<String> getIsNotPrimitiveOutput() {
        return isNotPrimitiveOutput;
    }

    public void addIsNotPrimitiveOutput(String isNotPrimitiveOutput) {
        this.isNotPrimitiveOutput.add(isNotPrimitiveOutput);
    }
}
