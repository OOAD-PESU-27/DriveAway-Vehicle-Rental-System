import { FaCheckCircle } from 'react-icons/fa';

const steps = ['Select Dates', 'Location', 'Review', 'Payment'];

interface Props { currentStep: number; }

export default function BookingSteps({ currentStep }: Props) {
  return (
    <div className="flex items-center justify-center mb-10">
      {steps.map((step, index) => {
        const stepNum = index + 1;
        const isCompleted = stepNum < currentStep;
        const isCurrent = stepNum === currentStep;
        return (
          <div key={step} className="flex items-center">
            <div className="flex flex-col items-center">
              <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm transition-all ${isCompleted ? 'bg-green-500 text-white' : isCurrent ? 'bg-blue-700 text-white' : 'bg-gray-200 text-gray-500'}`}>
                {isCompleted ? <FaCheckCircle /> : stepNum}
              </div>
              <span className={`text-xs mt-2 font-medium ${isCurrent ? 'text-blue-700' : 'text-gray-500'}`}>{step}</span>
            </div>
            {index < steps.length - 1 && (
              <div className={`w-16 sm:w-24 h-1 mx-2 mb-5 rounded transition-colors ${stepNum < currentStep ? 'bg-green-500' : 'bg-gray-200'}`} />
            )}
          </div>
        );
      })}
    </div>
  );
}
