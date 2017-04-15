package se.snylt.witch.viewbinder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import android.app.Activity;
import android.view.View;

import se.snylt.witch.viewbinder.viewfinder.ViewFinder;

import static junit.framework.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WitchTest {

    @Mock
    WitchCore core;

    @Mock
    Object target;

    @Mock
    Activity activity;

    @Mock
    View activityContentView;

    @Mock
    View view;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
        when(activity.findViewById(android.R.id.content)).thenReturn(activityContentView);
        Witch.witch(core);
    }

    @Test
    public void bind_WhitActivity_Should_Call_WitchCore_doBind_With_Target_And_ViewBinderWithActivityRoot(){
        // When
        Witch.bind(target, activity);

        // Then
        verify(core).doBind(same(target), viewFinderWith(activityContentView));
    }

    @Test
    public void bind_WhitView_Should_Call_WitchCore_doBind_With_Target_And_ViewBinderWithView(){
        // When
        Witch.bind(target, view);

        // Then
        verify(core).doBind(same(target), viewFinderWith(view));
    }

    @Test
    public void bind_Should_Call_WitchCore_doBind_With_Target_And_ViewBinderWithDefaultTag(){
        // When
        Witch.bind(target, view);

        // Then
        verify(core).doBind(same(target), viewFinderWithTag(Witch.VIEW_HOLDER_TAG_DEFAULT));
    }

    @Test
    public void spellBind_WithActivity_Should_DoWhateverBindDoes(){
        // When
        Witch.spellBind(target, activity);

        // Then
        verify(core).doBind(same(target), viewFinderWithTag(Witch.VIEW_HOLDER_TAG_DEFAULT));
    }

    @Test
    public void spellBind_WithView_Should_DoWhateverBindDoes(){
        // When
        Witch.spellBind(target, view);

        // Then
        verify(core).doBind(same(target), viewFinderWithTag(Witch.VIEW_HOLDER_TAG_DEFAULT));
    }

    private ViewFinder viewFinderWithTag(final int tag) {
        return argThat(new ArgumentMatcher<ViewFinder>() {
            @Override
            public boolean matches(ViewFinder argument) {
                return argument.getTag() == tag;
            }
        });
    }

    @Test
    public void bind_WhitMods_Should_Call_witchCore_doBind_With_Mods(){
        // When
        Object mod1 = new Object();
        Object mod2 = new Object();
        Witch.bind(target, view);

        // Then
        verify(core).doBind(same(target), viewFinderWith(view));

        fail();
    }

    private ViewFinder viewFinderWith(final View view) {
        return argThat(new ArgumentMatcher<ViewFinder>() {
            @Override
            public boolean matches(ViewFinder argument) {
                return argument.getRoot() == view;
            }
        });
    }

}
