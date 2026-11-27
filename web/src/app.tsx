import { useEffect } from 'preact/hooks';
import { data, profileId, revealing, screen, replaceAll } from './data/store';
import { AddListScreen } from './screens/AddList';
import { HomeScreen } from './screens/Home';
import { CreateProfileScreen, ProfilesScreen, TutorialScreen } from './screens/Onboarding';
import { CatalogScreen, GuideScreen, MeScreen, SearchScreen } from './screens/Others';
import { RevealLayer, revealClip, SplashScreen } from './screens/Splash';
import { Toast } from './ui/kit';
import { setSoundEnabled, unlockAudio } from './ui/sound';

function Current() {
  const s = screen.value;
  // Si se pierde la sesión (por ejemplo, borraron el perfil), regresamos a elegir perfil.
  const needsProfile = ['home', 'guide', 'catalog', 'search', 'me'].includes(s.name) && !profileId.value;
  useEffect(() => {
    if (needsProfile) replaceAll({ name: data.value.profiles.length ? 'profiles' : 'tutorial' });
  }, [needsProfile]);
  if (needsProfile) return null;
  switch (s.name) {
    case 'splash':
      return <SplashScreen />;
    case 'tutorial':
      return <TutorialScreen />;
    case 'createProfile':
      return <CreateProfileScreen key={s.editId ?? 'nuevo'} firstTime={s.firstTime} editId={s.editId} />;
    case 'addList':
      return <AddListScreen firstTime={s.firstTime} />;
    case 'profiles':
      return <ProfilesScreen />;
    case 'home':
      return <HomeScreen />;
    case 'guide':
      return <GuideScreen />;
    case 'catalog':
      return <CatalogScreen key={s.kind} kind={s.kind} />;
    case 'search':
      return <SearchScreen />;
    case 'me':
      return <MeScreen />;
  }
}

export function App() {
  useEffect(() => setSoundEnabled(data.value.settings.sound), [data.value.settings.sound]);
  useEffect(() => {
    const on = () => unlockAudio();
    window.addEventListener('pointerdown', on, { once: true });
    window.addEventListener('keydown', on, { once: true });
  }, []);
  useEffect(() => window.scrollTo(0, 0), [screen.value]);
  return (
    <>
      {revealing.value && <RevealLayer />}
      <div class="reveal-root" style={{ clipPath: revealClip.value ?? undefined }}>
        <Current />
      </div>
      <Toast />
    </>
  );
}
