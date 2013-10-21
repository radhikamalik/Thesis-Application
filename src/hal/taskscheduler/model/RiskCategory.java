package hal.taskscheduler.model;

public enum RiskCategory {

	OVERHEAD, HAND_ARM, LIFTING, PUSH_PULL, BENDING, KNEELING;

	@Override
	public String toString(){
		switch(this){
		case OVERHEAD: return "Overhead";
		case HAND_ARM: return "Hand/Arm";
		case LIFTING: return "Lifting";
		case PUSH_PULL: return "Push/Pull";
		case BENDING: return "Bending";
		case KNEELING: return "Kneeling";
		}
		return null;
	}
}
