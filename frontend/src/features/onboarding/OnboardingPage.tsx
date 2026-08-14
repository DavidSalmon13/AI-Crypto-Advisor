import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getOptions, getPreferences, savePreferences } from '../../api/preferencesApi';
import { Spinner } from '../../components/Spinner';
import type { ContentType, InvestorType } from '../../types/preferences';
import { useAuth } from '../auth/useAuth';
import { AssetsStep } from './steps/AssetsStep';
import { ContentStep } from './steps/ContentStep';
import { InvestorTypeStep } from './steps/InvestorTypeStep';

export function OnboardingPage() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [step, setStep] = useState(0);
  const [interests, setInterests] = useState<string[]>([]);
  const [investorType, setInvestorType] = useState<InvestorType | null>(null);
  const [contentTypes, setContentTypes] = useState<ContentType[]>([]);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const optionsQuery = useQuery({ queryKey: ['preferences-options'], queryFn: getOptions });

  // Prefill when editing existing answers; a 404 just means first-time onboarding.
  const existingQuery = useQuery({
    queryKey: ['preferences'],
    queryFn: getPreferences,
    retry: false,
  });

  useEffect(() => {
    if (existingQuery.data) {
      setInterests(existingQuery.data.interests);
      setInvestorType(existingQuery.data.investorType);
      setContentTypes(existingQuery.data.contentTypes);
    }
  }, [existingQuery.data]);

  const saveMutation = useMutation({
    mutationFn: savePreferences,
    onSuccess: (saved) => {
      queryClient.setQueryData(['preferences'], saved);
      navigate('/dashboard', { replace: true });
    },
    onError: () => setSubmitError('Could not save your preferences. Please try again.'),
  });

  if (optionsQuery.isLoading) {
    return <Spinner />;
  }

  if (optionsQuery.isError || !optionsQuery.data) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-100">
        <p className="text-sm text-slate-600">Could not load the onboarding options. Please refresh.</p>
      </div>
    );
  }

  const options = optionsQuery.data;
  const stepValid =
    (step === 0 && interests.length >= 1 && interests.length <= 10) ||
    (step === 1 && investorType !== null) ||
    (step === 2 && contentTypes.length >= 1);

  const handleNext = () => {
    if (step < 2) {
      setStep(step + 1);
      return;
    }
    if (investorType) {
      setSubmitError(null);
      saveMutation.mutate({ investorType, interests, contentTypes });
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-lg rounded-lg bg-white p-8 shadow-md">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-slate-900">Set up your dashboard</h1>
          <button
            type="button"
            onClick={handleLogout}
            className="rounded border border-slate-300 px-3 py-1.5 text-sm text-slate-700"
          >
            Log out
          </button>
        </div>

        <p className="mb-6 text-sm text-slate-500">Step {step + 1} of 3</p>

        {step === 0 && (
          <AssetsStep coins={options.coins} selected={interests} onChange={setInterests} />
        )}
        {step === 1 && (
          <InvestorTypeStep
            options={options.investorTypes}
            selected={investorType}
            onChange={setInvestorType}
          />
        )}
        {step === 2 && (
          <ContentStep options={options.contentTypes} selected={contentTypes} onChange={setContentTypes} />
        )}

        {submitError && <p className="mt-4 text-sm text-red-600">{submitError}</p>}

        <div className="mt-6 flex justify-between">
          <button
            type="button"
            onClick={() => setStep(Math.max(0, step - 1))}
            disabled={step === 0}
            className="rounded border border-slate-300 px-4 py-2 text-sm text-slate-700 disabled:opacity-40"
          >
            Back
          </button>
          <button
            type="button"
            onClick={handleNext}
            disabled={!stepValid || saveMutation.isPending}
            className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
          >
            {step < 2 ? 'Next' : saveMutation.isPending ? 'Saving…' : 'Finish'}
          </button>
        </div>
      </div>
    </div>
  );
}
