package com.fpt.edu.schedule.ai.model;

import com.fpt.edu.schedule.ai.lib.Class;
import com.fpt.edu.schedule.ai.lib.*;
import lombok.Data;
import org.jscience.mathematics.number.Real;
import org.jscience.mathematics.vector.DenseMatrix;
import org.jscience.mathematics.vector.DenseVector;

import java.util.*;

@Data
public class Chromosome {
    public static final double[] W = {0.8, 0.2};

    private Vector<Vector<Integer>> genes;
    private boolean needTobeUpdated;
    private Model model;
    private double fitness;


    public void updateFitness() {
        this.needTobeUpdated = true;
    }

    public void calculateFitness2() {

//        System.out.println(1);
        Vector<SlotGroup> slots = this.model.getSlots();

        int S1 = 0;
        for (SlotGroup slotGroup : this.model.getSlots()) {
            int total = 0;
            for (Teacher teacher : this.model.getTeachers()) {
                int teachingSlotNumber = 0;
                for (Slot slot : slotGroup.getSlots()) {
                    if (this.genes.get(slot.getId()).get(teacher.getId()) != -1) {
                        teachingSlotNumber++;
                    }
                }
//                System.out.println(teachingSlotNumber);
                if (teachingSlotNumber == 1) total++;
            }
            S1 += total * slotGroup.getCoff();
        }
        int S2 = 0;
        for (SlotGroup slotGroup : this.model.getSlots()) {
            int total = 0;
            for (Teacher teacher : this.model.getTeachers()) {
                int changeNumber = 0;
                String last = "";

                for (Slot slot : slotGroup.getSlots()) {
                    if (this.genes.get(slot.getId()).get(teacher.getId()) != -1) {
                        int classId = this.genes.get(slot.getId()).get(teacher.getId());
                        String building = this.model.getClasses().get(classId).getRoom().getBuilding();
                        if (!building.equalsIgnoreCase(last)) {
                            changeNumber++;
                            last = building;
                        }
                    }
                }
                total += Math.max(0, changeNumber - 1);
            }
            S2 += total * slotGroup.getCoff();
        }

//        assert(S1 != -1);

        this.fitness = W[0] / (1.0 + 1.0 * S1 / this.model.getTeachers().size())
                + W[1] / (1.0 + 1.0 * S2 / this.model.getTeachers().size());

        this.needTobeUpdated = false;
    }

    public double calculateSatisfaction(int teacherId) {
        Vector<Integer> vt = new Vector<>();
        Vector<Slot> slots = SlotGroup.getSlotList(this.model.getSlots());
        for (int j = 0; j < slots.size(); j++) {
            vt.add(this.genes.get(j).get(teacherId));
        }
        //o1
        double slotSatisfaction = 0;
        double maxSlotSatisfaction = 0;
        double max = 0;
        for (int i = 0; i < slots.size(); i++) max = Math.max(max, this.model.getRegisteredSlots()[teacherId][i]);
        for (int classId : vt) {
            if (classId != -1) {
                int slotId = this.model.getClasses().get(classId).getSlotId();
                slotSatisfaction += this.model.getRegisteredSlots()[teacherId][slotId];
                maxSlotSatisfaction += max;
            }
        }
//        maxSlotSatisfaction = Math.max(maxSlotSatisfaction, this.model.getTeachers().get(teacherId).getExpectedNumberOfClass() * max);
        //o2
        double subjectSatisfaction = 0;
        double maxSubjectSatisfaction = 0;
        max = 0;
        for (int i = 0; i < this.model.getSubjects().size(); i++)
            max = Math.max(max, this.model.getRegisteredSubjects()[teacherId][i]);

        for (int classId : vt) {
            if (classId != -1) {
                int subjectId = this.model.getClasses().get(classId).getSubjectId();
                subjectSatisfaction += this.model.getRegisteredSubjects()[teacherId][subjectId];
                maxSubjectSatisfaction += max;
            }
        }
//        maxSubjectSatisfaction = Math.max(maxSubjectSatisfaction, this.model.getTeachers().get(teacherId).getExpectedNumberOfClass() * max);
        //o3
        double numberOfClassSatisfaction = 0;
        int cnt = 0;
        for (int classId : vt) {
            if (classId != -1) {
                cnt++;
            }
        }

        //o4

        double distance = 0;
        Room lastRoom = null;
        for (int i = 0; i < 6; i++) {
            int classId = vt.get(i);
            if (classId != -1) {
                if (lastRoom == null) {
                    lastRoom = this.model.getClasses().get(classId).getRoom();
                } else {
                    distance += lastRoom.distance(this.model.getClasses().get(classId).getRoom()) * 3;
                    lastRoom = this.model.getClasses().get(classId).getRoom();
                }
            }
        }
        lastRoom = null;
        for (int i = 6; i < 10; i++) {
            int classId = vt.get(i);
            if (classId != -1) {
                if (lastRoom == null) {
                    lastRoom = this.model.getClasses().get(classId).getRoom();
                } else {
                    distance += lastRoom.distance(this.model.getClasses().get(classId).getRoom()) * 2;
                    lastRoom = this.model.getClasses().get(classId).getRoom();
                }
            }
        }

        //o5
        Set<String> set = new HashSet<>();
        for (int classId : vt) {
            if (classId != -1) {
                set.add(this.model.getClasses().get(classId).getStudentGroup());
            }
        }

        int o5 = cnt - set.size();
        // cnt - set.size()

        //o6
        int consecutiveSlot = 0;
        int overLimit = 0;
        int lim = this.model.getTeachers().get(teacherId).getConsecutiveSlotLimit();
        for (int i = 0; i < 6; i++) {
            int classId = vt.get(i);
            if (classId != -1) {
                consecutiveSlot += 1;
            } else {
                consecutiveSlot = 0;
            }
            if (consecutiveSlot > lim) overLimit += 3;
        }
        consecutiveSlot = 0;
        if (vt.get(6) != -1) consecutiveSlot = 2;
        else consecutiveSlot = 0;
        if (consecutiveSlot > lim) overLimit += consecutiveSlot - lim;
        if (vt.get(7) != -1) consecutiveSlot++;
        else consecutiveSlot = 0;
        if (consecutiveSlot > lim) overLimit++;
        if (vt.get(8) != -1) consecutiveSlot += 2;
        else consecutiveSlot = 0;
        if (consecutiveSlot > lim) overLimit++;
        if (vt.get(9) != -1) consecutiveSlot += 1;
        else consecutiveSlot = 0;
        if (consecutiveSlot > lim) overLimit++;
        consecutiveSlot = 0;
        if (vt.get(6) != -1) consecutiveSlot = 1;
        else consecutiveSlot = 0;
        if (consecutiveSlot > lim) overLimit++;
        if (vt.get(7) != -1) consecutiveSlot += 2;
        else consecutiveSlot = 0;
        if (consecutiveSlot > lim) overLimit++;
        if (vt.get(8) != -1) consecutiveSlot += 1;
        else consecutiveSlot = 0;
        if (consecutiveSlot > lim) overLimit++;
        if (vt.get(9) != -1) consecutiveSlot += 2;
        else consecutiveSlot = 0;
        if (consecutiveSlot > lim) overLimit++;

        double slotCoff = this.model.getGaParameter().getCofficient().getSlotCoff();
        double subjectCoff = this.model.getGaParameter().getCofficient().getSubjectCoff();
        double numberOfClassCoff = this.model.getGaParameter().getCofficient().getNumberOfClassCoff();
        double distanceCoff = this.model.getGaParameter().getCofficient().getDistanceCoff();
        double consecutiveClassCoff = this.model.getGaParameter().getCofficient().getConsicutiveClassCoff();
        double F = slotCoff * (maxSlotSatisfaction == 0 ? 0 : 1.0 * slotSatisfaction / maxSlotSatisfaction) +
                subjectCoff * (maxSubjectSatisfaction == 0 ? 0 : 1.0 * subjectSatisfaction / maxSubjectSatisfaction) +
                numberOfClassCoff * 1.0 / (1.0 + Math.abs(cnt - this.model.getTeachers().get(teacherId).getExpectedNumberOfClass())) +
                distanceCoff * 1.0 / (1.0 + 9.0 * distance / 105.0) +
                consecutiveClassCoff * 1.0 / (1.0 + o5);
        return F;
    }

    public double calculateFitnessForSubGroupUsingScalarizingModel(Vector<Teacher> teachers) {
        //o1: maximize do hai long cua giang vien voi slot
        //o2: maximize do hai long cua giang vien voi mon hoc
        //o3: minimize chenh lech so lop duoc xep voi so lop ki vong
        //o4: minimize khoang cach di chuyen
        //o5: minimize so giang vien day 1 lop qua 2 mon
        //o6: minimize so giang vien day qua gioi han

        if (teachers.size() == 0) return 1.0;
        double p[] = new double[teachers.size()];

        double total = 0;

        for (int i = 0; i < teachers.size(); i++) {
            p[i] = calculateSatisfaction(teachers.get(i).getId());
            total += p[i];
        }

        double F = 0;
        for (int i = 0; i < teachers.size(); i++) {
            F += p[i];
        }

        double F1 = 0;
        double minSatisfaction = p[0];
        double maxSatisfaction = p[0];

        double variance = 0;
        for (int i = 0; i < teachers.size(); i++) {
            for (int j = 0; j < teachers.size(); j++) {
                F1 += Math.abs(p[i] - p[j]);
            }
            variance += Math.pow(p[i] - total / teachers.size(), 2);
            minSatisfaction = Math.min(p[i], minSatisfaction);
            maxSatisfaction = Math.max(p[i], maxSatisfaction);

        }
        double std = (teachers.size() == 1) ? 0.0 : Math.sqrt(variance / (teachers.size() - 1));
        F1 = maxSatisfaction - minSatisfaction;

        double satisfactionSumCoff = this.model.getGaParameter().getCofficient().getSatisfactionSumCoff();
        double stdCoff = this.model.getGaParameter().getCofficient().getStdCoff();
        double fitness = F / teachers.size() * satisfactionSumCoff +
                (1.0 / (1.0 + std * 5.0)) * stdCoff;
        return fitness;
    }

    double distance(DenseMatrix a, DenseMatrix b) {
        double res = 0;
        for (int i = 0; i < a.getNumberOfRows(); i++) {
            for (int j = 0; j < a.getNumberOfColumns(); j++) {
                Real x = (Real) a.get(i, j);
                Real y = (Real) b.get(i, j);
                res += Math.pow(Math.abs(x.doubleValue() - y.doubleValue()), 2);
            }
        }
        return Math.sqrt(res);
    }

    public double calculateFitnessForSubGroupUsingCompromisingModel(Vector<Teacher> teachers) {
        if (teachers.size() == 0) return 1.0;
        Vector<DenseVector<Real>> expectedMatrix = new Vector<>();
        int[][] slotSubject = new int[10][this.model.getSubjects().size()];

        for (Class _class : this.model.getClasses()) {
            slotSubject[_class.getSlotId()][_class.getSubjectId()] = 1;
        }
        for (int i = 0; i < teachers.size(); i++) {
            int teacherId = teachers.get(i).getId();
            Vector<Real> row = new Vector<>();
            for (int slotId = 0; slotId < 10; slotId++) {
                double expectedThisSlot = 0;
                for (int j = 0; j < this.model.getSubjects().size(); j++) {
                    expectedThisSlot = Math.max(expectedThisSlot, slotSubject[slotId][j] * this.model.getRegisteredSubjects()[teacherId][j]);
                }
                row.add(Real.valueOf(expectedThisSlot * this.model.getRegisteredSlots()[teacherId][slotId]));
            }
            row.add(Real.valueOf(Math.pow(this.model.getTeachers().get(teacherId).getExpectedNumberOfClass(), 2)));


            expectedMatrix.add(DenseVector.valueOf(row));
        }
        DenseMatrix expected = DenseMatrix.valueOf(expectedMatrix);

        Vector<DenseVector<Real>> factMatrix = new Vector<>();
        for (int i = 0; i < teachers.size(); i++) {
            int teacherId = teachers.get(i).getId();
            int numberOfClassAssigned = 0;
            Vector<Real> row = new Vector<>();
            for (int j = 0; j < 10; j++) {
                int classId = this.getGenes().get(j).get(teacherId);
                if (classId == -1) {
                    row.add(Real.ZERO);
                } else {
                    numberOfClassAssigned++;
                    int subjectId = this.model.getClasses().get(classId).getSubjectId();
                    row.add(Real.valueOf(this.model.getRegisteredSubjects()[teacherId][subjectId] *
                            this.model.getRegisteredSlots()[teacherId][j]));
                }
            }
            row.add(Real.valueOf(numberOfClassAssigned * numberOfClassAssigned));
            factMatrix.add(DenseVector.valueOf(row));
        }

        DenseMatrix fact = DenseMatrix.valueOf(factMatrix);
        Vector<DenseVector<Real>> zeroMatrix = new Vector<>();
        for (int i = 0; i < teachers.size(); i++) {
            int teacherId = teachers.get(i).getId();
            Vector<Real> row = new Vector<>();
            for (int slotId = 0; slotId < 10; slotId++) {
                double expectedThisSlot = 0;
                for (int j = 0; j < this.model.getSubjects().size(); j++) {
                    expectedThisSlot = Math.max(expectedThisSlot, slotSubject[slotId][j] * this.model.getRegisteredSubjects()[teacherId][j]);
                }
                row.add(Real.valueOf(((expectedThisSlot * 2 > 25) ? 0 : 25) * this.model.getRegisteredSlots()[teacherId][slotId]));
            }
            row.add(Real.valueOf(Math.pow((this.model.getTeachers().get(teacherId).getExpectedNumberOfClass() * 2 > 10 ? 0 : 10), 2)));
            row.add(Real.ZERO);
            zeroMatrix.add(DenseVector.valueOf(row));
        }

        DenseMatrix zeroMat = DenseMatrix.valueOf(zeroMatrix);
        return 1.0 - distance(expected, fact) / distance(expected, zeroMat);
    }

    public int getNumberOfViolation() {
        int rs = 0;
        for (Teacher teacher : this.getModel().getTeachers()) {
            int teacherId = teacher.getId();
            int cnt = 0;
            for (int j = 0; j < 10; j++) {
                if (this.genes.get(j).get(teacherId) != -1) {
                    cnt++;
                }
            }
            if (cnt < this.model.getTeachers().get(teacherId).getQuota()) rs++;
        }
        return rs;
    }


    public double calculateFitness() {


        Vector<Teacher> fullTimeTeachers = new Vector<>();
        Vector<Teacher> partTimeTeachers = new Vector<>();
        for (Teacher teacher : this.model.getTeachers()) {
            if (teacher.getType() == Teacher.FULL_TIME) {
                fullTimeTeachers.add(teacher);
            } else partTimeTeachers.add(teacher);
        }

        double fulltimeCoff = this.model.getGaParameter().getCofficient().getFulltimeCoff();
        double parttimeCoff = this.model.getGaParameter().getCofficient().getParttimeCoff();
        double objectiveValue = 0.0;
        switch (this.model.getGaParameter().getModelType()) {
            case (Model.SCALARIZING):
                objectiveValue = fulltimeCoff * this.calculateFitnessForSubGroupUsingScalarizingModel(fullTimeTeachers) +
                        parttimeCoff * this.calculateFitnessForSubGroupUsingScalarizingModel(partTimeTeachers);
                break;
            case (Model.COMPROMISING):
                objectiveValue = fulltimeCoff * this.calculateFitnessForSubGroupUsingCompromisingModel(fullTimeTeachers) +
                        parttimeCoff * this.calculateFitnessForSubGroupUsingCompromisingModel(partTimeTeachers);
                break;
            default: {
                objectiveValue = fulltimeCoff * this.calculateFitnessForSubGroupUsingScalarizingModel(fullTimeTeachers) + parttimeCoff * this.calculateFitnessForSubGroupUsingScalarizingModel(partTimeTeachers);
            }
        }
        double hardConstrainCoff = this.model.getGaParameter().getCofficient().getHardConstraintCoff();
        double softConstrainCoff = this.model.getGaParameter().getCofficient().getSoftConstraintCoff();
        this.fitness = hardConstrainCoff * (1.0 / (1.0 + this.getNumberOfViolation())) + softConstrainCoff * objectiveValue;
        this.needTobeUpdated = false;
        return fitness;
    }

    public double getFitness() {
        if (needTobeUpdated) {
            this.calculateFitness();
        }

        return this.fitness;
    }

    Vector<Integer> getClassBySlot(Vector<Class> classes, int slotId) {
        Vector<Integer> res = new Vector<>();
        for (Class c : classes) {
            if (c.getSlotId() == slotId && c.getStatus() == Class.OK) {
                res.add(c.getId());
            }
        }

        return res;
    }


    public Chromosome(Model model) {
        this.model = model;
        this.needTobeUpdated = true;

        int m = model.getTeachers().size();
        int n = model.getClasses().size();

        Vector<Slot> slots = SlotGroup.getSlotList(this.model.getSlots());

        Vector<Vector<Integer>> genes = new Vector<>();
        for (int i = 0; i < slots.size(); i++) {
            Vector<Integer> classes = getClassBySlot(model.getClasses(), slots.get(i).getId());
            while (classes.size() < m) classes.add(-1);
            Collections.shuffle(classes);
            genes.add(classes);
        }
        this.genes = genes;
        this.autoRepair();
    }

    public Chromosome(Model model, Vector<Vector<Integer>> genes) {
        this.model = model;
        this.needTobeUpdated = true;
        this.genes = genes;
        this.autoRepair();
    }

    public void autoRepair() {
        Vector<Slot> slots = SlotGroup.getSlotList(this.model.getSlots());
        for (int i = 0; i < slots.size(); i++) {
            Vector<Integer> col = this.genes.get(i);
            HopcroftKarp hp = new HopcroftKarp(this.model.getTeachers().size(), this.model.getClasses().size());

            for (int j = 0; j < model.getTeachers().size(); j++) {
                if (model.getRegisteredSlots()[j][i] > 0) {
                    for (int k = 0; k < col.size(); k++) {
                        if (col.get(k) != -1) {
                            int subjectId = model.getClasses().get(col.get(k)).getSubjectId();
                            if (this.model.getRegisteredSubjects()[j][subjectId] > 0) {
                                hp.add(j, col.get(k));
                            }
                        }
                    }
                }
            }

            for (int j = 0; j < model.getTeachers().size(); j++) {
                int teacherId = this.model.getTeachers().get(j).getId();
                int classId = col.get(j);
                if (classId != -1) {
                    int subjectId = this.model.getClasses().get(classId).getSubjectId();
                    if (this.model.getRegisteredSubjects()[teacherId][subjectId] > 0 &&
                            this.model.getRegisteredSlots()[teacherId][i] > 0) {
                        hp.match(teacherId, classId);
                    }
                }
            }
            int[] matching = hp.getMatching();
            for (int j = 0; j < model.getTeachers().size(); j++) {
                this.genes.get(i).set(j, matching[j]);
            }
        }
    }

//    public void autoRepair1() {
//        Vector<Graph.Edge> edges = new Vector<>();
//        Vector<Slot> slots = SlotGroup.getSlotList(this.model.getSlots());
//        int source = this.model.getTeachers().size() * (slots.size() + 1) + this.model.getClasses().size();
//        int sink = source + 1;
//        int superSource1 = sink + 1;
//        int superSource = superSource1 + 1;
//        int superSink = superSource + 1;
//
//        Dinic dinic = new Dinic(superSink + 1, superSource, superSink);
//        for (int i = 0; i < this.model.getTeachers().size(); i++) {
//            if (this.model.getTeachers().get(i).getType() == Teacher.FULL_TIME) {
//                edges.add(new Graph.Edge(source, i, this.model.getTeachers().get(i).getQuota(), Dinic.INF));
//            } else {
//                edges.add(new Graph.Edge(source, i, 0, Dinic.INF));
//            }
//        }
//
//        for (int i = 0; i < this.model.getClasses().size(); i++) {
//            edges.add(new Graph.Edge(this.model.getTeachers().size() * (1 + slots.size()) + i, sink, 0, 1));
//        }
//
//        for (int i = 0; i < this.model.getTeachers().size(); i++) {
//            for (int j = 0; j < slots.size(); j++) {
//                if (this.model.getRegisteredSlots()[i][j] == 0) continue;
//                dinic.add(i, this.model.getTeachers().size() + i * slots.size() + j, 1);
//                Vector<Integer> col = this.genes.get(j);
//                for (int k = 0; k < col.size(); k++) {
//                    int classId = col.get(k);
//                    if (classId != -1) {
//                        int subjectId = this.model.getClasses().get(classId).getSubjectId();
//                        if (this.model.getRegisteredSubjects()[i][subjectId] > 0) {
//                            edges.add(new Graph.Edge(this.model.getTeachers().size() + i * slots.size() + j,
//                                    this.model.getTeachers().size() * (1 + slots.size()) + classId, 0, 1));
//                        }
//                    }
//                }
//            }
//        }
//
//        for (Graph.Edge edge : edges) {
//            dinic.add(edge.u, edge.v, edge.d, edge.c);
//        }
//        dinic.add(sink, superSource1, Dinic.INF);
//
//        for (int j = 0; j < slots.size(); j++) {
//            Vector<Integer> col = this.genes.get(j);
//            for (int i = 0; i < this.model.getTeachers().size(); i++) {
//                int classId = col.get(i);
//                if (classId != -1) {
//                    int subjectId = this.model.getClasses().get(classId).getSubjectId();
//                    if (this.model.getRegisteredSubjects()[i][subjectId] > 0) {
//                        dinic.match(this.model.getTeachers().size() + i * slots.size() + j,
//                                this.model.getTeachers().size() * (slots.size() + 1) + classId);
//                    }
//                }
//            }
//        }
//        int totalDemand = 0;
//        for (int i = 0; i < this.model.getTeachers().size(); i++) {
//            totalDemand += this.model.getTeachers().get(i).getQuota();
//        }
//        dinic.add(superSource1, source, this.model.getClasses().size(), Dinic.INF);
//
//        int fl = dinic.maxflow();
//
//        if (fl != this.model.getClasses().size() + totalDemand) {
//            System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx " + fl);
//        }
//        int[][] flow = dinic.flow;
//
//        for (int j = 0; j < slots.size(); j++) {
//            for (int i = 0; i < this.model.getTeachers().size(); i++) {
//                this.getGenes().get(j).set(i, -1);
//            }
//            int cnt = 0;
//
//            for (int i = 0; i < this.model.getTeachers().size(); i++) {
//                Vector<Integer> classes = this.getClassBySlot(this.model.getClasses(), j);
////                int cnt = 0;
//                for (int classId : classes) {
//                    int u = this.model.getTeachers().size() + i * slots.size() + j;
//                    int v = this.model.getTeachers().size() * (slots.size() + 1) + classId;
//                    if (flow[u][v] > 0) {
//                        this.getGenes().get(j).set(i, classId);
//                        cnt++;
//                    }
//                }
//            }
//        }
//    }

    public void mutate() {
        int m = this.model.getTeachers().size();
        Random random = new Random();
        int x = random.nextInt(m);
        int y = random.nextInt(m);
        Vector<Slot> slots = SlotGroup.getSlotList(this.model.getSlots());
        int slotId = random.nextInt(slots.size());
        int tmp = this.genes.get(slotId).get(x);
        this.genes.get(slotId).set(x, this.genes.get(slotId).get(y));
        this.genes.get(slotId).set(y, tmp);
    }

    public Vector<Record> getSchedule() {
        Vector<Record> rs = new Vector<>();
        for (int i = 0; i < this.getModel().getTeachers().size(); i++) {
            for (int j = 0; j < 10; j++) {
                int classId = this.getGenes().get(j).get(i);
                if (classId != -1) {
                    int subjectId = this.model.getClasses().get(classId).getSubjectId();
                    //validate class before adding to result
                    if (this.model.getRegisteredSlots()[i][j] > 0 && this.model.getRegisteredSubjects()[i][subjectId] > 0) {
                        rs.add(new Record(this.getModel().getTeacherIdReverse(i), this.getModel().getClassIdReverse(classId),
                                this.getModel().getSubjectIdReverse(subjectId),
                                this.getModel().getSlotIdReverse(this.getModel().getClasses().get(classId).getSlotId())));
                    }
                }
            }
        }
        return rs;
    }
}
